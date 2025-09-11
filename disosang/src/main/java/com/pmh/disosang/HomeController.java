package com.pmh.disosang;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "천안 사랑 카드 - 착한 가격 업소 지도 서비스");
        return "index"; // templates/index.html 호출
    }
}