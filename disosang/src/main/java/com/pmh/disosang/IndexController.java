package com.pmh.disosang;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
    @GetMapping("/") // 웹사이트의 가장 첫 페이지 (루트 경로)
    public String welcomePage() {
        return "welcome"; // templates/welcome.html 호출
    }
}
