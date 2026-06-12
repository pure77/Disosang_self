package com.pmh.disosang.favorite.service;

import com.pmh.disosang.favorite.FavoriteRepository;
import com.pmh.disosang.favorite.entity.Favorite;
import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User userWithId(Long id) {
        return User.builder().id(id).name("u").email("u@e.com").password("p").build();
    }

    @Test
    void 즐겨찾기가_없으면_추가하고_true를_반환한다() {
        User user = userWithId(1L);
        Store store = Store.builder().storeId(10L).placeName("가게").storeType("일반음식점").build();
        given(favoriteRepository.findByUser_IdAndStore_StoreId(1L, 10L)).willReturn(Optional.empty());
        given(storeRepository.findById(10L)).willReturn(Optional.of(store));

        boolean result = favoriteService.toggle(user, 10L);

        assertThat(result).isTrue();
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void 이미_즐겨찾기면_삭제하고_false를_반환한다() {
        User user = userWithId(1L);
        Favorite existing = mock(Favorite.class);
        given(favoriteRepository.findByUser_IdAndStore_StoreId(1L, 10L)).willReturn(Optional.of(existing));

        boolean result = favoriteService.toggle(user, 10L);

        assertThat(result).isFalse();
        verify(favoriteRepository).delete(existing);
        verify(storeRepository, never()).findById(any());
    }
}
