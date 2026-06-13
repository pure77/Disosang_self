package com.pmh.disosang.map.home;

import com.pmh.disosang.favorite.service.FavoriteService;
import com.pmh.disosang.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final FavoriteService favoriteService;

    @GetMapping("/home/home")
    public String home(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("favorites",
                user != null ? favoriteService.getFavoriteStores(user.getId()) : List.of());
        return "home/home";
    }
}
