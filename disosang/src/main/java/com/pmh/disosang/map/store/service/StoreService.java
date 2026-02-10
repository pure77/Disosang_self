package com.pmh.disosang.map.store.service;


import com.pmh.disosang.map.store.CategoryRepository;
import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.entity.Store;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
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

        if (minLat > maxLat || minLng > maxLng) {

        }
       // 검색어로 카테고리 후보 찾기
        List<Long> rootCategoryIds =
                categoryRepository.findCategoryIdsByKeyword(keyword);

        //하위 카테고리까지 확장
        Set<Long> categoryIds = new HashSet<>();
        for (Long rootId : rootCategoryIds) {
            categoryIds.addAll(
                    categoryRepository.findAllDescendantIds(rootId)
            );
        }

        // 카테고리 못 찾았을 경우 대비
        if (categoryIds.isEmpty()) {
            categoryIds.add(-1L); // IN 절 에러 방지용 더미
        }
        // 1차 시도: 지도 내 키워드 검색
        List<Store> stores = storeRepository.findStoresInAreaByCategoryAndKeywordOrderedByDistance(
                keyword,new ArrayList<>(categoryIds),centerLat, centerLng, minLat, maxLat, minLng, maxLng
        );

        // 2차 시도: 지도 내 검색 결과가 없을 경우, 전체에서 키워드로 검색
        if (stores.isEmpty()) {
            stores = storeRepository.findStoresByKeyword(keyword, centerLat, centerLng);
        }


        return stores.stream()
                .map(StoreResponse::fromEntity)
                .collect(Collectors.toList());

    }

    //가게 상세 정보 조회
    @Transactional(readOnly = true)
    public StoreResponse findById(long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 가게가 없습니다"));

        // 2. DTO에 정의된 fromEntity 메서드를 사용해 변환 후 반환합니다.
        return StoreResponse.fromEntity(store);


    }

}
