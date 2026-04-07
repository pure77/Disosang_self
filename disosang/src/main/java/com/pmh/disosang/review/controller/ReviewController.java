package com.pmh.disosang.review.controller;

import com.pmh.disosang.review.dto.request.ReviewRequest;
import com.pmh.disosang.review.service.ReviewService;
import com.pmh.disosang.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/add")
    public String addReview(@Valid @ModelAttribute ReviewRequest reviewRequest,
                            BindingResult bindingResult,
                            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                            @AuthenticationPrincipal User user,
                            RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/user/login";
        }

        Long redirectStoreId = reviewRequest.getStoreId();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return redirectStoreId != null
                    ? "redirect:/store/detail/" + redirectStoreId + "#panel-review"
                    : "redirect:/home/home";
        }

        try {
            reviewService.createReview(reviewRequest, photos, user);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 성공적으로 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/store/detail/" + redirectStoreId + "#panel-review";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 등록 중 오류가 발생했습니다.");
            return "redirect:/store/detail/" + redirectStoreId + "#panel-review";
        }

        return "redirect:/store/detail/" + redirectStoreId + "#panel-review";
    }

    @PostMapping("/edit/{reviewId}")
    public String editReview(@PathVariable("reviewId") long reviewId,
                             @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                             @RequestParam(value = "deletedPhotos", required = false) List<String> deletedPhotos,
                             @Valid @ModelAttribute ReviewRequest reviewRequest,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes redirectAttributes) {
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }

        Long storeId = reviewRequest.getStoreId();
        if (storeId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "잘못된 요청입니다. (가게 정보 누락)");
            return "redirect:/home/home";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/store/detail/" + storeId + "#panel-review";
        }

        try {
            reviewService.updateReview(reviewId, reviewRequest, photos, deletedPhotos, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 성공적으로 수정되었습니다.");
            return "redirect:/store/detail/" + storeId + "#panel-review";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/store/detail/" + storeId + "#panel-review";
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/store/detail/" + storeId + "#panel-review";
        } catch (Exception e) {
            log.error("리뷰 수정 중 예상하지 못한 오류 발생", e);
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰 수정 중 오류가 발생했습니다.");
            return "redirect:/store/detail/" + storeId + "#panel-review";
        }
    }

    @PostMapping("/delete/{reviewId}")
    public String deleteReview(@PathVariable("reviewId") long reviewId,
                               RedirectAttributes redirectAttributes) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = null;

        if (principal instanceof User) {
            currentUser = (User) principal;
        } else {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        try {
            long storeId = reviewService.deleteReview(reviewId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 삭제되었습니다.");
            return "redirect:/store/detail/" + storeId + "#panel-review";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/home/home";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/home/home";
        }
    }
}
