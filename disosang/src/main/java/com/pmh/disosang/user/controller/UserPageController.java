package com.pmh.disosang.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserPageController {

    @GetMapping("/userInfo")
    public String userInfoPage(Model model) {
        model.addAttribute("message", "내 정보 페이지");
        return "user/userInfo";

    }
    // 회원가입 페이지를 보여주는 GET 요청
    @GetMapping("/signup")
    public String signupPage() {
        return "user/signup"; // templates/signup.html 반환
    }

    @GetMapping("/login")
    public String loginPage() {
        return "user/login"; // templates/login.html 호출
    }
}
