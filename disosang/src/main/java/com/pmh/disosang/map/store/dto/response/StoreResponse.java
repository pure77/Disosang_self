package com.pmh.disosang.map.store.dto.response;

import com.pmh.disosang.map.store.entity.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 가맹점 응답용 DTO (클라이언트 응답 전용)
 */
@Getter
@AllArgsConstructor
@Builder
public class StoreResponse {
    private Long id;
    private String placeName;
    private String category;
    private String addressName;
    private String roadAddressName;
    private String phone;
    private  String storeType;
    private Double x;  // 경도
    private Double y;  // 위도
    private Double averageRating;        // 별점
    private Integer reviewCount;  // 리뷰 수
    private String thumbnailUrl;  // 썸네일 이미지 URL
    public static StoreResponse fromEntity(Store store) {
        return StoreResponse.builder()
                .id(store.getStoreId())
                .placeName(store.getPlaceName())
                .category(store.getCategory())
                .addressName(store.getAddressName())
                .roadAddressName(store.getRoadAddressName())
                .phone(store.getPhone())
                .storeType(store.getStoreType())
                .x(store.getX())
                .y(store.getY())
                .averageRating(store.getAverageRating())
                .reviewCount(store.getReviewCount())
                // 썸네일 URL도 엔티티에서 가져오도록 추가하는 것이 좋습니다.
                // .thumbnailUrl(store.getThumbnailUrl())
                .build();
    }

}
