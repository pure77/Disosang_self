package com.pmh.disosang.review.dto.response;

import com.pmh.disosang.review.entity.Review;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ReviewResponse {
    private Long reviewId;
    private Integer rating;
    private String content;
    private String authorName; // User 엔티티에서 이름만 추출
    private List<String> photoUrls; // Photo 엔티티에서 URL만 추출
    private boolean isMine; // 현재 로그인한 유저가 작성했는지 여부
    public ReviewResponse(Review review,boolean isMine) {
        this.reviewId = review.getReviewId();
        this.rating = review.getRating();
        this.content = review.getContent();
        this.authorName = review.getUser().getName(); // User 객체에서 이름 가져오기
        this.photoUrls = review.getPhotos().stream()
                .map(photo -> photo.getFileUrl())
                .collect(Collectors.toList());
        this.isMine = isMine;
    }
}