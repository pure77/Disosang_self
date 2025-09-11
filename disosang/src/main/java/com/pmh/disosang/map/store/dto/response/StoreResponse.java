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

    public static StoreResponse fromEntity(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .placeName(store.getPlaceName())
                .category(store.getCategory())
                .addressName(store.getAddressName())
                .roadAddressName(store.getRoadAddressName())
                .phone(store.getPhone())
                .storeType(store.getStoreType())
                .x(store.getX())
                .y(store.getY())
                .build();
    }
}
