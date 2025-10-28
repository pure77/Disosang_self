package com.pmh.disosang.map.store.dto.response;

import com.pmh.disosang.review.dto.response.ReviewResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class StoreDetailResponse {
    private StoreResponse storeInfo; // 기존에 사용하시던 가게 정보 DTO
    private List<ReviewResponse> reviews; // 가게에 달린 리뷰 DTO 목록

    public StoreDetailResponse(StoreResponse storeInfo, List<ReviewResponse> reviews) {
        this.storeInfo = storeInfo;
        this.reviews = reviews;
    }
}