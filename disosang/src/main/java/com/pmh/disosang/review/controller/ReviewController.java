package com.pmh.disosang.review.controller;


import com.pmh.disosang.review.dto.request.ReviewRequest;
import com.pmh.disosang.review.service.ReviewService; // ✅ Service 임포트
import com.pmh.disosang.user.entity.User; // ✅ User 엔티티 임포트
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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
        System.out.println("리다이렉트할 storeId: {}" + redirectStoreId);
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

    @PostMapping("/edit/{reviewId}")
    public String editReview(
            @PathVariable("reviewId") long reviewId,
            @ModelAttribute ReviewRequest reviewRequest, // 1. 폼 데이터를 받을 DTO
            @AuthenticationPrincipal User currentUser, // 2. 현재 로그인 사용자
            RedirectAttributes redirectAttributes
    ) {
        // 2. 비로그인 사용자 확인
        if (currentUser == null) {
            // 이 예외는 보통 Spring Security의 예외 처리기가 처리하지만,
            // 핸들러에 도달했다면 명시적으로 처리할 수 있습니다.
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login"; // 로그인 페이지로 이동
        }

        // 3. 리다이렉트할 storeId를 DTO에서 미리 가져옵니다. (deleteReview와 다른 점)
        //    addReview와 마찬가지로 DTO에 storeId가 있다고 가정합니다.

        Long storeId = reviewRequest.getStoreId();
        System.out.println("리뷰 리퀘스트 스토어아디"+storeId);
        // (방어 코드) DTO에 storeId가 없는 비정상적 접근 처리
        if (storeId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "잘못된 접근입니다. (가게 정보 누락)");
            return "redirect:/home/home"; // 메인 홈으로
        }

        try {
            // 4. 서비스에 수정 로직 위임 (권한 검사 포함)
            reviewService.updateReview(reviewId, reviewRequest, currentUser);

            // 5. 성공 시 리다이렉트 및 메시지 전달
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 성공적으로 수정되었습니다.");
            // ✅ 성공 시, 미리 받아둔 storeId를 사용하여 가게 상세 페이지로 이동
            return "redirect:/store/detail/" + storeId + "#panel-review";

        } catch (AccessDeniedException e) {
            // 6. 권한 없는 경우 (남의 글 수정 시도)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // ✅ 실패 시에도, 미리 받아둔 storeId를 사용하여 가게 상세 페이지로 이동
            return "redirect:/store/detail/" + storeId + "#panel-review";

        } catch (EntityNotFoundException e) {
            // 7. 리뷰가 없는 경우
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage()); // "해당 리뷰를 찾을 수 없습니다."
            // ✅ 실패 시에도, 미리 받아둔 storeId를 사용하여 가게 상세 페이지로 이동
            return "redirect:/store/detail/" + storeId + "#panel-review";

        } catch (Exception e) {
            // 8. 그 외 알 수 없는 예외 처리
            log.error("리뷰 수정 중 알 수 없는 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 수정 중 오류가 발생했습니다.");
            return "redirect:/store/detail/" + storeId + "#panel-review";
        }
    }
    @PostMapping("/delete/{reviewId}")
    public String deleteReview(@PathVariable("reviewId") long reviewId, // 'deleteReivew' -> 'deleteReview' 오타 수정
                               RedirectAttributes redirectAttributes) {

        // 1. 현재 로그인한 사용자 정보를 가져옵니다.
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = null;

        if (principal instanceof User) {
            currentUser = (User) principal;
        } else {
            // 비로그인 상태면 권한 없음 예외 발생
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        try {
            // 2. 서비스에 삭제 요청 (권한 확인 포함)
            //    삭제 후 리다이렉트할 storeId를 반환받습니다.
            long storeId = reviewService.deleteReview(reviewId, currentUser);

            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 삭제되었습니다.");

            // 3. 성공 시, 해당 가게 상세 페이지로 리다이렉트
            return "redirect:/store/detail/" + storeId + "#panel-review";

        } catch (AccessDeniedException e) {
            // 4. 서비스에서 권한 없음 예외가 발생한 경우 (남의 글 삭제 시도)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // (storeId를 모르므로 홈으로 리다이렉트 하거나, 예외에서 storeId를 받아와야 함)
            return "redirect:/home/home";
        } catch (IllegalArgumentException e) {
            // 4. 리뷰가 존재하지 않는 경우
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/home/home";
        }
    }
    }

