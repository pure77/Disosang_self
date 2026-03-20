package com.pmh.disosang.map.store.service;

import com.pmh.disosang.map.store.CategoryRepository;
import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    public List<StoreResponse> searchStoresInMap(String keyword, double centerLat, double centerLng,
                                                 double minLat, double maxLat,
                                                 double minLng, double maxLng) {

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        List<Long> nearbyStoreIds = storeRepository.findNearbyStoreIds(minLat, maxLat, minLng, maxLng);
        if (nearbyStoreIds.isEmpty()) {
            return List.of();
        }

        List<Store> stores = List.of();
        List<Long> categoryIds = getExpandedCategoryIds(normalizedKeyword);

        if (!categoryIds.isEmpty()) {
            stores = storeRepository.findNearbyStoresByCategoryIdsOrderedByDistance(
                    nearbyStoreIds,
                    categoryIds,
                    centerLat,
                    centerLng
            );
        }

        if (stores.isEmpty()) {
            stores = storeRepository.findNearbyStoresByKeywordOrderedByDistance(
                    nearbyStoreIds,
                    normalizedKeyword,
                    centerLat,
                    centerLng
            );
        }

        return stores.stream()
                .map(StoreResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private List<Long> getExpandedCategoryIds(String keyword) {
        List<Long> rootCategoryIds = categoryRepository.findCategoryIdsByExactName(keyword);
        if (rootCategoryIds.isEmpty()) {
            rootCategoryIds = categoryRepository.findCategoryIdsByKeyword(keyword);
        }

        Set<Long> categoryIds = new LinkedHashSet<>();
        for (Long rootId : rootCategoryIds) {
            categoryIds.addAll(categoryRepository.findAllDescendantIds(rootId));
        }

        return new ArrayList<>(categoryIds);
    }
    @Transactional(readOnly = true)
    public StoreResponse findById(long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("ID를 찾을수 없습니다"));

        return StoreResponse.fromEntity(store);
    }
}
