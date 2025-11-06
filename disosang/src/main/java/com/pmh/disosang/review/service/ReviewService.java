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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    public List<ReviewResponse> getReviews(long storeId) {
// (1) 현재 로그인중한 사용자 정보 가져오기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = null; // 비로그인 상태(anonymousUser)를 대비해 null로 초기화

        // principal이 User 객체인지 확인 후 캐스팅
        if (principal instanceof User) {
            currentUser = (User) principal;
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));

        // (2) 람다에서 사용하기 위해 final 또는 effectively final 변수가 필요
        User finalCurrentUser = currentUser;

        return reviewRepository.findByStoreOrderByReviewIdDesc(store)
                .stream()
                // (3) ReviewResponse::new를 람다식으로 변경
                .map(review -> {

                    boolean isMine = false; // 기본값은 false (내 리뷰 아님)

                    // (4) 로그인한 사용자(finalCurrentUser)가 있고, 리뷰 작성자(review.getUser())도 있는지 확인
                    if (finalCurrentUser != null && review.getUser() != null) {

                        // (5) 두 사용자의 고유 ID를 비교 (getId()는 Long을 반환한다고 가정)
                        isMine = review.getUser().getId().equals(finalCurrentUser.getId());
                    }

                    // (6) isMine 값을 포함하여 새 생성자(new ReviewResponse(review, isMine))를 호출
                    return new ReviewResponse(review, isMine);
                })
                .collect(Collectors.toList());
    }

    public long deleteReview(long reviewId, User currentUser) {

        // 1. 리뷰를 DB에서 찾습니다. (Store 정보까지 fetch join하면 더 좋음)
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다: " + reviewId));

        // 2. [중요] 권한 확인: 리뷰 작성자와 현재 로그인한 유저가 같은지 확인
        if (review.getUser() == null || !review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("리뷰를 삭제할 권한이 없습니다.");
        }

        // 3. Store 엔티티를 가져옵니다.
        Store store = review.getStore();
        if (store == null) {
            throw new IllegalArgumentException("리뷰에 연결된 가게 정보가 없습니다.");
        }

        // 4. [핵심] store의 평점/리뷰 수를 '삭제' 모드로 업데이트합니다.
        store.updateRating(review, false); // 'false' = 삭제

        // 5. 변경된 store 정보를 DB에 저장합니다.
        storeRepository.save(store);

        // 6. 리뷰를 DB에서 삭제합니다.
        reviewRepository.delete(review);

        // 7. 리다이렉트를 위해 storeId 반환
        return store.getStoreId();
    }


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
