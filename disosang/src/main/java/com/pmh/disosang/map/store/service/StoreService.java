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

        List<Store> stores = new ArrayList<>();
        List<Long> rootCategoryIds = categoryRepository.findCategoryIdsByExactName(normalizedKeyword);
        if (rootCategoryIds.isEmpty()) {
            rootCategoryIds = categoryRepository.findCategoryIdsByKeyword(normalizedKeyword);
        }

        Set<Long> categoryIds = new LinkedHashSet<>();
        for (Long rootId : rootCategoryIds) {
            categoryIds.addAll(categoryRepository.findAllDescendantIds(rootId));
        }

        if (!categoryIds.isEmpty()) {
            stores = storeRepository.findStoresInAreaByCategoryIdsOrderedByDistance(
                    new ArrayList<>(categoryIds),
                    centerLat,
                    centerLng,
                    minLat,
                    maxLat,
                    minLng,
                    maxLng
            );
        }

        if (stores.isEmpty()) {
            stores = storeRepository.findStoresInAreaByKeywordOrderedByDistance(
                    normalizedKeyword,
                    centerLat,
                    centerLng,
                    minLat,
                    maxLat,
                    minLng,
                    maxLng
            );
        }

        if (stores.isEmpty()) {
            stores = storeRepository.findStoresByKeyword(normalizedKeyword, centerLat, centerLng);
        }

        return stores.stream()
                .map(StoreResponse::fromEntity)
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public StoreResponse findById(long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 가게가 없습니다"));

        return StoreResponse.fromEntity(store);
    }
}
