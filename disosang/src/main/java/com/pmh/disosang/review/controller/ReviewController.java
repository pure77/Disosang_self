package com.pmh.disosang.review.controller;


import com.pmh.disosang.review.dto.request.ReviewRequest;
import com.pmh.disosang.review.service.ReviewService; // ✅ Service 임포트
import com.pmh.disosang.user.entity.User; // ✅ User 엔티티 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // ✅ 리다이렉트 시 메시지 전달용

import java.util.List;
@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/add")
    public String addReview(@ModelAttribute ReviewRequest reviewRequest,
                            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                            @AuthenticationPrincipal User user,
                            RedirectAttributes redirectAttributes) { // ✅ 메시지 전달 위한 파라미터 추가

        // 로그인하지 않은 사용자는 리뷰를 등록할 수 없도록 처리 (SecurityConfig에서 이미 막지만, 추가 검증)
        if (user == null) {
            // 로그인 페이지로 보내거나 에러 메시지 처리
            return "redirect:/login";
        }
        Long redirectStoreId = reviewRequest.getStoreId();
        System.out.println("리다이렉트할 storeId: {}"+ redirectStoreId);
        try {
            reviewService.createReview(reviewRequest, photos, user);
            // 성공 메시지 추가 (선택 사항)
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 성공적으로 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            // 가게 정보가 없거나 하는 등의 예외 발생 시 에러 메시지 전달
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // 에러 발생 시, 가게 상세 페이지로 다시 돌아가되 placeId를 사용
            return "redirect:/store/detail/" + redirectStoreId;
        } catch (Exception e) {
            // 그 외 예상치 못한 에러 처리
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 등록 중 오류가 발생했습니다.");
            return "redirect:/store/detail/" + redirectStoreId;
        }


        // ✅ 리뷰 등록 후, DTO에 포함된 placeId를 이용해 해당 가게 상세 페이지로 리다이렉트
        return "redirect:/store/detail/" + redirectStoreId;
    }
}
