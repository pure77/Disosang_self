package com.pmh.disosang.map.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home/home")
    public String home(Model model) {
        model.addAttribute("message", "천안 사랑 카드 - 착한 가격 업소 지도 서비스");
        return "home/home"; // templates/home.html 호출
    }
}