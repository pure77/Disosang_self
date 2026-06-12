package com.pmh.disosang.favorite.controller;

import com.pmh.disosang.favorite.dto.request.FavoriteToggleRequest;
import com.pmh.disosang.favorite.service.FavoriteService;
import com.pmh.disosang.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggle(@RequestBody FavoriteToggleRequest request,
                                    @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }
        if (request == null || request.storeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "가게 정보가 필요합니다."));
        }

        boolean favorite = favoriteService.toggle(user, request.storeId());
        return ResponseEntity.ok(Map.of("favorite", favorite));
    }
}
