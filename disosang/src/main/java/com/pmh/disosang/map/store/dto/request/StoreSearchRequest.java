package com.pmh.disosang.map.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreSearchRequest {

    @NotBlank(message = "검색어는 필수입니다.")
    @Schema(description = "검색 키워드", example = "카페")
    private String keyword;

    @NotNull(message = "centerY는 필수입니다.")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Schema(description = "지도 중심 위도", example = "36.84950309992622")
    private Double centerY;

    @NotNull(message = "centerX는 필수입니다.")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Schema(description = "지도 중심 경도", example = "127.15437257867464")
    private Double centerX;

    @NotNull(message = "minY는 필수입니다.")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Schema(description = "지도 최소 위도", example = "36.845258941966016")
    private Double minY;

    @NotNull(message = "maxY는 필수입니다.")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Schema(description = "지도 최대 위도", example = "36.8530782657718")
    private Double maxY;

    @NotNull(message = "minX는 필수입니다.")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Schema(description = "지도 최소 경도", example = "127.14723334692333")
    private Double minX;

    @NotNull(message = "maxX는 필수입니다.")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Schema(description = "지도 최대 경도", example = "127.16278946667573")
    private Double maxX;

    @AssertTrue(message = "지도 범위 값이 올바르지 않습니다.")
    public boolean isValidBounds() {
        if (minY == null || maxY == null || minX == null || maxX == null) {
            return true;
        }
        return minY <= maxY && minX <= maxX;
    }

    @AssertTrue(message = "중심 좌표는 현재 지도 범위 안에 있어야 합니다.")
    public boolean isCenterInsideBounds() {
        if (centerY == null || centerX == null || minY == null || maxY == null || minX == null || maxX == null) {
            return true;
        }
        return centerY >= minY && centerY <= maxY
                && centerX >= minX && centerX <= maxX;
    }
}
