package com.pmh.disosang.favorite.service;

import com.pmh.disosang.favorite.FavoriteRepository;
import com.pmh.disosang.favorite.entity.Favorite;
import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StoreRepository storeRepository;

    /**
     * 즐겨찾기 토글
     * - 이미 즐겨찾기한 가게면 삭제하고 false 반환
     * - 아니면 새로 추가하고 true 반환
     */
    public boolean toggle(User user, Long storeId) {
        return favoriteRepository.findByUser_IdAndStore_StoreId(user.getId(), storeId)
                .map(existing -> {
                    favoriteRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    Store store = storeRepository.findById(storeId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));
                    favoriteRepository.save(Favorite.builder().user(user).store(store).build());
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long storeId) {
        return favoriteRepository.existsByUser_IdAndStore_StoreId(userId, storeId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getFavoriteStoreIds(Long userId) {
        return new HashSet<>(favoriteRepository.findStoreIdsByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getFavoriteStores(Long userId) {
        return favoriteRepository.findByUserIdWithStore(userId).stream()
                .map(favorite -> StoreResponse.fromEntity(favorite.getStore()))
                .collect(Collectors.toList());
    }
}
