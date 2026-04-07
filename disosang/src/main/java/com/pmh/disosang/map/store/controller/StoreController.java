package com.pmh.disosang.map.store.controller;

import com.pmh.disosang.map.store.dto.request.StoreSearchRequest;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.service.StoreService;
import com.pmh.disosang.review.dto.response.ReviewResponse;
import com.pmh.disosang.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final ReviewService reviewService;

    @Value("${kakao.maps.js.key}")
    private String kakaoJsKey;

    @GetMapping("/map")
    public String mapPage(Model model) {
        model.addAttribute("kakaoKey", kakaoJsKey);
        return "store/map";
    }

    @GetMapping("/map/search")
    public ResponseEntity<?> searchStoresInMap(@Valid @ModelAttribute StoreSearchRequest request,
                                               BindingResult bindingResult) {
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
        return ResponseEntity.ok(storeResponses);
    }

    @GetMapping("/detail/{storeId}")
    public String storeDetail(@PathVariable("storeId") Long storeId,
                              @RequestParam(name = "sort", defaultValue = "newest") String sort,
                              Model model) {
        StoreResponse storeInfo = storeService.findById(storeId);
        List<ReviewResponse> reviews = reviewService.getReviews(storeId, sort);

        model.addAttribute("store", storeInfo);
        model.addAttribute("reviews", reviews);
        model.addAttribute("sort", sort);
        return "store/detail";
    }
}
