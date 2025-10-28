package com.pmh.disosang.review.service;

import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.review.ReviewRepository;
import com.pmh.disosang.review.dto.request.ReviewRequest;
import com.pmh.disosang.review.dto.response.ReviewResponse;
import com.pmh.disosang.review.entity.Photo;
import com.pmh.disosang.review.entity.Review;
import com.pmh.disosang.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public void createReview(ReviewRequest reviewRequest,List<MultipartFile> photos ,User user) {
        log.info("리뷰 생성 시작: storeId={}, rating={}, userEmail={}", reviewRequest.getStoreId(), reviewRequest.getRating(), user.getEmail()); // DTO 값 로깅
        Store store = storeRepository.findById(reviewRequest.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));
        log.info("가게 조회 완료: storeId={}", store.getStoreId());
        // 2. Photo 파일 업로드 처리 (가상)
        List<Photo> photoEntities = uploadPhotos(photos, store, user);
        log.info("사진 업로드 처리 완료: {}개", photoEntities.size()); // 👈 사진 처리 확인
        Review review = new Review();
        review.setUser(user);
        review.setStore(store);

        review.setRating(reviewRequest.getRating());
        review.setContent(reviewRequest.getContent());
        log.info("Review 엔티티 생성 완료: rating={}", review.getRating()); // 👈 엔티티 생성 확인
        //Review와 Photo 연관관계 설정 (CascadeType.ALL 덕분에 Review 저장 시 Photo도 저장됨)
        for (Photo photo :photoEntities) {
            photo.setReview(review);// Photo 엔티티에 Review를 연결
        }
        log.info("사진-리뷰 연관관계 설정 완료");
        review.setPhotos(photoEntities);

        Review savedReview = reviewRepository.save(review);
        log.info("리뷰 저장 완료: reviewId = {}", savedReview.getReviewId()); // 👈 저장 직후 로그 (ID 확인 중요!)
        store.updateRating(savedReview,true);

        storeRepository.save(store);
        log.info("가게 평균 별점 업데이트 완료: storeId={}, newAvgRating={}", store.getStoreId(), store.getAverageRating()); // 👈 가게 업데이트 확인

        log.info("리뷰 생성 종료");
    }

    //특정 가게 리뷰 조회
    @Transactional
    public List<ReviewResponse> getReviews(long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));

        return reviewRepository.findByStoreOrderByReviewIdDesc(store)
                .stream()
                .map(ReviewResponse::new) // Review 엔티티를 ReviewResponse DTO로 변환
                .collect(Collectors.toList());
    }

    @Transactional
    List<Photo> uploadPhotos(List<MultipartFile> files, Store store, User user) {
        List<Photo> photoEntities = new ArrayList<>();

        //파일 없거나 비어있으면 빈 리스트반환
        if (files == null || files.isEmpty()) {
            return photoEntities;
        }

        for (MultipartFile file : files) {
            if(files.isEmpty()) continue;

            // 1. (가상) 파일을 서버 어딘가에 저장 (예: S3, 로컬 스토리지)
            // String savedUrl = "https://s3.example.com/" + file.getOriginalFilename();
            // String savedFileName = file.getOriginalFilename();

            // 2. Photo 엔티티 생성
            Photo photo = new Photo();
            // photo.setFileUrl(savedUrl);
            // photo.setFileName(savedFileName);
            photo.setFileUrl("https://via.placeholder.com/150?text=" + file.getOriginalFilename()); // 임시 이미지 URL
            photo.setStore(store);
            // (Photo 엔티티에 User 연결이 있다면)
            // photo.setUser(user);

            photoEntities.add(photo);
        }
        return photoEntities;
    }
}
