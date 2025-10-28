package com.pmh.disosang.map.store.service;


import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.entity.Store;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;


    public List<StoreResponse> searchStoresInMap(String keyword, double centerLat, double centerLng,
                                                 double minLat, double maxLat,
                                                 double minLng, double maxLng) {

        if (minLat > maxLat || minLng > maxLng) {

        }
        // 1차 시도: 지도 내 키워드 검색
        List<Store> stores = storeRepository.findStoresInAreaByKeywordOrderedByDistance(
                keyword, centerLat, centerLng, minLat, maxLat, minLng, maxLng
        );

        // 2차 시도: 지도 내 검색 결과가 없을 경우, 전체에서 키워드로 검색
        if (stores.isEmpty()) {
            stores = storeRepository. findStoresByKeyword(keyword, centerLat, centerLng);
        }
        //검색 되는 가게 없을시
        if (stores.isEmpty()) {

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
