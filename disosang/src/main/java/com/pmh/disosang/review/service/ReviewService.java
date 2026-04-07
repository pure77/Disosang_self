package com.pmh.disosang.review.service;

import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.review.ReviewRepository;
import com.pmh.disosang.review.dto.request.ReviewRequest;
import com.pmh.disosang.review.dto.response.ReviewResponse;
import com.pmh.disosang.review.entity.Photo;
import com.pmh.disosang.review.entity.Review;
import com.pmh.disosang.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final FileService fileService;

    public void createReview(ReviewRequest reviewRequest, List<MultipartFile> photos, User user) {
        Store store = storeRepository.findById(reviewRequest.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));

        List<Photo> photoEntities = uploadPhotos(photos, store);

        Review review = new Review();
        review.setUser(user);
        review.setStore(store);
        review.setRating(reviewRequest.getRating());
        review.setContent(reviewRequest.getContent());

        for (Photo photo : photoEntities) {
            photo.setReview(review);
        }

        review.setPhotos(photoEntities);

        Review savedReview = reviewRepository.save(review);
        store.updateRating(savedReview, true);
        storeRepository.save(store);
    }

    public List<ReviewResponse> getReviews(long storeId, String sort) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = null;

        if (principal instanceof User) {
            currentUser = (User) principal;
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));

        User finalCurrentUser = currentUser;

        Sort sortCondition = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null) {
            switch (sort) {
                case "oldest" -> sortCondition = Sort.by(Sort.Direction.ASC, "createdAt");
                case "rating-high" -> sortCondition = Sort.by(Sort.Direction.DESC, "rating");
                case "rating-low" -> sortCondition = Sort.by(Sort.Direction.ASC, "rating");
                default -> sortCondition = Sort.by(Sort.Direction.DESC, "createdAt");
            }
        }

        return reviewRepository.findReviewsByStoreWithFetchJoin(store, sortCondition)
                .stream()
                .map(review -> {
                    boolean isMine = false;
                    if (finalCurrentUser != null && review.getUser() != null) {
                        isMine = review.getUser().getId().equals(finalCurrentUser.getId());
                    }
                    return new ReviewResponse(review, isMine);
                })
                .collect(Collectors.toList());
    }

    public long deleteReview(long reviewId, User currentUser) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다. " + reviewId));

        if (review.getUser() == null || !review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("리뷰를 삭제할 권한이 없습니다.");
        }

        Store store = review.getStore();
        if (store == null) {
            throw new IllegalArgumentException("리뷰에 연결된 가게 정보가 없습니다.");
        }

        store.updateRating(review, false);
        storeRepository.save(store);
        reviewRepository.delete(review);

        return store.getStoreId();
    }

    public void updateReview(long reviewId, ReviewRequest reviewRequest,
                             List<MultipartFile> newPhotos, List<String> deletedPhotos,
                             User currentUser) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("이 리뷰를 수정할 권한이 없습니다.");
        }

        Store store = review.getStore();
        store.updateEditRating(review.getRating(), reviewRequest.getRating());

        if (deletedPhotos != null && !deletedPhotos.isEmpty()) {
            review.getPhotos().removeIf(photo -> {
                if (deletedPhotos.contains(photo.getFileUrl())) {
                    fileService.deleteFile(photo.getFileUrl());
                    return true;
                }
                return false;
            });
        }

        if (newPhotos != null && !newPhotos.isEmpty()) {
            List<Photo> addedPhotos = uploadPhotos(newPhotos, store);
            for (Photo photo : addedPhotos) {
                photo.setReview(review);
                review.getPhotos().add(photo);
            }
        }

        review.update(reviewRequest.getContent(), reviewRequest.getRating());
    }

    List<Photo> uploadPhotos(List<MultipartFile> files, Store store) {
        List<Photo> photoEntities = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return photoEntities;
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            try {
                String savedFileUrl = fileService.uploadFile(file);
                String saveFileName = file.getOriginalFilename();

                Photo photo = new Photo();
                photo.setFileUrl(savedFileUrl);
                photo.setFileName(saveFileName);
                photo.setStore(store);
                photoEntities.add(photo);
            } catch (IOException e) {
                log.error("파일 업로드 처리 중 IO 오류 발생: {}", file.getOriginalFilename(), e);
            }
        }

        return photoEntities;
    }
}
