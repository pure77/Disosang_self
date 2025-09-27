package com.pmh.disosang.user.controller;

import com.pmh.disosang.user.dto.request.UserSignupRequest;
import com.pmh.disosang.user.entity.User;
import com.pmh.disosang.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public String signup(UserSignupRequest signupRequest, RedirectAttributes redirectAttributes) {
        try {
            userService.signup(signupRequest);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/signup";
        }

        // 회원가입 성공 시 로그인 페이지로 이동
        return "redirect:/login";
    }

    @GetMapping("/userInfo")
    public String userInfo(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();

        User user = userService.findByEmailUserInfo(email);

        model.addAttribute("user", user);

        return "user/userInfo";
    }
}
