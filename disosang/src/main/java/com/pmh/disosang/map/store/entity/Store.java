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



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "road_address_name")
    private  String roadAddressName;
    @Column(length = 20)
    private String phone;

    @Column(name = "x")
    private Double x;  // 경도 (longitude)

    @Column(name = "y")
    private Double y;  // 위도 (latitude)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "store_type")
    @NotNull
    private  String storeType;

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
