package com.pmh.disosang.map.store.controller;

import com.pmh.disosang.map.store.dto.request.StoreSearchRequest;
import com.pmh.disosang.map.store.dto.response.StoreDetailResponse;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.service.StoreService;
import com.pmh.disosang.review.dto.response.ReviewResponse;
import com.pmh.disosang.review.entity.Review;
import com.pmh.disosang.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final ReviewService reviewService;
    @Value("${kakao.maps.js.key}")
    private String kakaoJsKey;

    // map.html 반환
    @GetMapping("/map")
    public String mapPage(Model model) {
        model.addAttribute("kakaoKey", kakaoJsKey);
        return "store/map";
    }

    // REST API: 지도 기반 가맹점 검색
    @GetMapping("/map/search")
    public ResponseEntity<List<StoreResponse>> searchStoresInMap(@Valid @ModelAttribute StoreSearchRequest request) {
        List<StoreResponse> storeResponses = storeService.searchStoresInMap(
                request.getKeyword().trim(),
                request.getCenterY(),
                request.getCenterX(),
                request.getMinY(),
                request.getMaxY(),
                request.getMinX(),
                request.getMaxX()
        );
        return ResponseEntity.ok(storeResponses);
    }

    @GetMapping("/detail/{storeId}")
    public String storeDetail(@PathVariable("storeId") Long storeId, Model model) {
        // 1. DTO로 가게 정보를 조회합니다.
        StoreResponse storeInfo = storeService.findById(storeId);

        // 2. 가게 ID(PK)를 사용하여 리뷰 DTO 목록을 조회합니다.
        List<ReviewResponse> reviews = reviewService.getReviews(storeId); // ✅ getReviewsByStoreId 대신 getReviews 사용

        // 3. 가게 정보 DTO와 리뷰 DTO 목록을 하나의 래퍼(Wrapper) DTO에 담습니다.
        StoreDetailResponse pageData = new StoreDetailResponse(storeInfo, reviews);

        // 4. ✅ Model에 이 DTO 하나를 "pageData"라는 이름으로 전달합니다.
        model.addAttribute("pageData", pageData);;
        // 3. ✅ Model에 "store"와 "reviews" 라는 이름으로 각각 데이터를 담아 전달합니다.
        model.addAttribute("store", storeInfo);
        model.addAttribute("reviews", reviews);
        return "store/detail";
    }

}
