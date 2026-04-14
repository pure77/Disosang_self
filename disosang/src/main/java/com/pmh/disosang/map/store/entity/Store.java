package com.pmh.disosang.map.store.entity;


import com.pmh.disosang.review.entity.Review;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    /*
     * DB에 미리 만들어 둔 정규화 검색 컬럼입니다.
     * - 기존에는 native query 에서만 사용했지만,
     *   token 적재와 Java 재정렬에서도 같은 기준 문자열을 써야 하므로 엔티티에 읽기 전용으로 매핑합니다.
     * - 애플리케이션이 직접 쓰지 않고 DB 값을 읽기만 하므로 insert/update 대상에서는 제외합니다.
     */
    @Column(name = "place_name_search", insertable = false, updatable = false)
    private String placeNameSearch;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "address_name")
    private String addressName;

    /*
     * 지번 주소 정규화 검색 컬럼
     * - 주소 prefix 검색과 Java 재정렬에서 사용합니다.
     */
    @Column(name = "address_search", insertable = false, updatable = false)
    private String addressSearch;

    @Column(name = "road_address_name")
    private  String roadAddressName;

    /*
     * 도로명 주소 정규화 검색 컬럼
     * - 주소 prefix 검색과 Java 재정렬에서 사용합니다.
     */
    @Column(name = "road_address_search", insertable = false, updatable = false)
    private String roadAddressSearch;

    @Column(length = 20)
    private String phone;

    @Column(name = "lon")
    private Double x;  // 경도 (longitude)

    @Column(name = "lat")
    private Double y;  // 위도 (latitude)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "store_type")
    @NotNull
    private  String storeType;

    /*
     * 업종 정규화 검색 컬럼
     * - 업종 exact/prefix 검색과 Java 재정렬에서 사용합니다.
     */
    @Column(name = "store_type_search", insertable = false, updatable = false)
    private String storeTypeSearch;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    private Integer reviewCount =0;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // Store(1)이 Review(N)를 가짐
    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    public void updateRating(Review newReview, boolean isAdding) {
        if (isAdding) {
            // (기존 총점 + 새 별점) / (기존 리뷰 수 + 1)
            double totalRating = (this.averageRating * this.reviewCount) + newReview.getRating();
            this.reviewCount++;
            this.averageRating = totalRating / this.reviewCount;
        } else {
            // (기존 총점 - 삭제할 별점) / (기존 리뷰 수 - 1)
            double totalRating = (this.averageRating * this.reviewCount) - newReview.getRating();
            this.reviewCount--;
            this.averageRating = (this.reviewCount > 0) ? totalRating / this.reviewCount : 0.0;
        }
    }
    public void updateEditRating(int oldRating, int newRating) {
        // (기존 총점 - 옛날 별점 + 새 별점) / (기존 리뷰 수)
        double totalRating = (this.averageRating * this.reviewCount) - oldRating + newRating;
        // reviewCount는 변경되지 않습니다.

        if (this.reviewCount > 0) {
            this.averageRating = Math.round((totalRating / this.reviewCount) * 10.0) / 10.0;
        } else {
            this.averageRating = 0.0; // (수정 시 이 경우는 거의 없지만)
        }
    }

    @PrePersist
    protected void onCreate () {

        this.createdAt = LocalDateTime.now();
    }
}
