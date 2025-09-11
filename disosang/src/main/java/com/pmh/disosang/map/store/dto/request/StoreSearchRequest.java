package com.pmh.disosang.map.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 가맹점 요청용 DTO (예: 등록, 수정 등)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class StoreSearchRequest {
    @NotBlank(message = "검색 키워드는 필수입니다.")
    @Schema(description = "검색 키워드", example = "카페")
    private String keyword;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    @Schema(description = "지도 중심 위도", example = "36.84950309992622")
    private double centerY;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    @Schema(description = "지도 중심 경도", example = "127.15437257867464")
    private double centerX;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    @Schema(description = "지도 최소 위도", example = "36.845258941966016")
    private double minY;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    @Schema(description = "지도 최대 위도", example = "36.8530782657718")
    private double maxY;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    @Schema(description = "지도 최소 경도", example = "127.14723334692333")
    private double minX;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    @Schema(description = "지도 최대 경도", example = "127.16278946667573")
    private double maxX;

}
