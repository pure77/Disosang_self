package com.pmh.disosang.map.store.controller;

import com.pmh.disosang.favorite.service.FavoriteService;
import com.pmh.disosang.map.store.dto.request.StoreSearchRequest;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.service.StoreService;
import com.pmh.disosang.review.dto.response.ReviewResponse;
import com.pmh.disosang.review.service.ReviewService;
import com.pmh.disosang.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;

    @Value("${kakao.maps.js.key}")
    private String kakaoJsKey;

    @GetMapping("/map")
    public String mapPage(Model model) {
        model.addAttribute("kakaoKey", kakaoJsKey);
        return "store/map";
    }

    @GetMapping("/map/search")
    public ResponseEntity<?> searchStoresInMap(@Valid @ModelAttribute StoreSearchRequest request,
                                               BindingResult bindingResult,
                                               @AuthenticationPrincipal User user) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }

        List<StoreResponse> storeResponses = storeService.searchStoresInMap(
                request.getKeyword().trim(),
                request.getCenterY(),
                request.getCenterX(),
                request.getMinY(),
                request.getMaxY(),
                request.getMinX(),
                request.getMaxX()
        );

        // 로그인 사용자라면 즐겨찾기한 가게를 표시 (익명이면 모두 false 유지)
        if (user != null) {
            Set<Long> favoriteIds = favoriteService.getFavoriteStoreIds(user.getId());
            storeResponses.forEach(store -> store.setFavorite(favoriteIds.contains(store.getId())));
        }

        return ResponseEntity.ok(storeResponses);
    }

    @GetMapping("/detail/{storeId}/json")
    @ResponseBody
    public ResponseEntity<StoreResponse> storeDetailJson(@PathVariable("storeId") Long storeId,
                                                         @AuthenticationPrincipal User user) {
        StoreResponse storeInfo = storeService.findById(storeId);
        if (user != null) {
            storeInfo.setFavorite(favoriteService.isFavorite(user.getId(), storeId));
        }
        return ResponseEntity.ok(storeInfo);
    }

    @GetMapping("/detail/{storeId}")
    public String storeDetail(@PathVariable("storeId") Long storeId,
                              @RequestParam(name = "sort", defaultValue = "newest") String sort,
                              @AuthenticationPrincipal User user,
                              Model model) {
        StoreResponse storeInfo = storeService.findById(storeId);
        List<ReviewResponse> reviews = reviewService.getReviews(storeId, sort);

        boolean isFavorite = user != null && favoriteService.isFavorite(user.getId(), storeId);

        model.addAttribute("store", storeInfo);
        model.addAttribute("reviews", reviews);
        model.addAttribute("sort", sort);
        model.addAttribute("isFavorite", isFavorite);
        return "store/detail";
    }
}
