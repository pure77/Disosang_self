package com.pmh.disosang.map.store.controller;

import com.pmh.disosang.favorite.service.FavoriteService;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.service.StoreService;
import com.pmh.disosang.review.service.ReviewService;
import com.pmh.disosang.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock
    private StoreService storeService;
    @Mock
    private ReviewService reviewService;
    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private StoreController storeController;

    private StoreResponse storeWithId(Long id) {
        return StoreResponse.builder().id(id).placeName("가게").favorite(false).build();
    }

    @Test
    void 단일_가게_JSON은_로그인_사용자의_즐겨찾기를_표시한다() {
        given(storeService.findById(10L)).willReturn(storeWithId(10L));
        User user = User.builder().id(1L).name("u").email("u@e.com").password("p").build();
        given(favoriteService.isFavorite(1L, 10L)).willReturn(true);

        ResponseEntity<StoreResponse> res = storeController.storeDetailJson(10L, user);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isFavorite()).isTrue();
    }

    @Test
    void 익명_사용자는_즐겨찾기가_false이고_FavoriteService를_호출하지_않는다() {
        given(storeService.findById(10L)).willReturn(storeWithId(10L));

        ResponseEntity<StoreResponse> res = storeController.storeDetailJson(10L, null);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isFavorite()).isFalse();
        verifyNoInteractions(favoriteService);
    }
}
