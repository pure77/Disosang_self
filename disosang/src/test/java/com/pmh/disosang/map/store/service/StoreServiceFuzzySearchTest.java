package com.pmh.disosang.map.store.service;

import com.pmh.disosang.map.store.CategoryRepository;
import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreServiceFuzzySearchTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StoreSearchTokenService storeSearchTokenService;

    @InjectMocks
    private StoreService storeService;

    private static final double LAT = 36.8;
    private static final double LNG = 127.1;
    private static final double MIN_LAT = 36.7;
    private static final double MAX_LAT = 36.9;
    private static final double MIN_LNG = 127.0;
    private static final double MAX_LNG = 127.2;

    @BeforeEach
    void mockCommonDependencies() {
        given(categoryRepository.findCategoryIdsByExactName(any())).willReturn(List.of());
        given(categoryRepository.findCategoryIdsByKeyword(any())).willReturn(List.of());
    }

    // --- Tracer bullet ---

    @Test
    void 오타가_있는_3글자_키워드로_유사한_매장을_찾는다() {
        // "스터지" → 실제 매장명 "스터디" (편집거리 1)
        Store storeWithSimilarName = storeWithName(1L, "스터디");
        given(storeRepository.findExactNameStoresOrderedByDistance(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeRepository.findExactNameStoresGloballyOrderedByDistance(
                any(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeSearchTokenService.createQueryTokens(any())).willReturn(List.of("스터", "터지"));
        given(storeRepository.findNearbyStoresByKeywordOrderedByScore(
                any(), anyList(), anyLong(), anyList(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeRepository.findNearbyStoresForFuzzyMatching(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(List.of(storeWithSimilarName));

        List<StoreResponse> result = storeService.searchStoresInMap(
                "스터지", LAT, LNG, MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlaceName()).isEqualTo("스터디");
    }

    // --- 동작 2: 2글자 이하 키워드는 fuzzy 미발동 ---

    @Test
    void 두_글자_이하_키워드는_fuzzy_fallback이_발동하지_않는다() {
        // "치기" 2자 → fuzzy 조건 (length >= 3) 불충족
        given(storeSearchTokenService.createQueryTokens(any())).willReturn(List.of("치기"));
        given(storeRepository.findNearbyStoresByKeywordOrderedByScore(
                any(), anyList(), anyLong(), anyList(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeRepository.findExactNameStoresOrderedByDistance(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeRepository.findExactNameStoresGloballyOrderedByDistance(
                any(), anyDouble(), anyDouble()))
                .willReturn(List.of());

        List<StoreResponse> result = storeService.searchStoresInMap(
                "치기", LAT, LNG, MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG);

        assertThat(result).isEmpty();
        verify(storeRepository, never()).findNearbyStoresForFuzzyMatching(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    // --- 동작 3: 정상 검색 결과 있으면 fuzzy 미발동 ---

    @Test
    void 일반_검색_결과가_있으면_fuzzy_fallback이_발동하지_않는다() {
        Store normalResult = storeWithName(1L, "스터디카페");
        given(storeRepository.findExactNameStoresOrderedByDistance(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeRepository.findExactNameStoresGloballyOrderedByDistance(
                any(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeSearchTokenService.createQueryTokens(any())).willReturn(List.of("스터", "터디"));
        given(storeRepository.findNearbyStoresByKeywordOrderedByScore(
                any(), anyList(), anyLong(), anyList(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of(normalResult));

        List<StoreResponse> result = storeService.searchStoresInMap(
                "스터디", LAT, LNG, MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG);

        assertThat(result).isNotEmpty();
        verify(storeRepository, never()).findNearbyStoresForFuzzyMatching(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    // --- 동작 4: 편집거리가 maxDistance를 초과하는 매장은 결과에서 제외 ---

    @Test
    void 편집거리가_너무_큰_매장은_fuzzy_결과에_포함되지_않는다() {
        // "스터지" (3자, maxDistance=1) → "스터디"는 편집거리 1 (포함), "치킨집"은 편집거리 3 (제외)
        Store closeName = storeWithName(1L, "스터디");
        Store farName   = storeWithName(2L, "치킨집");
        given(storeRepository.findExactNameStoresOrderedByDistance(
                any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeRepository.findExactNameStoresGloballyOrderedByDistance(
                any(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeSearchTokenService.createQueryTokens(any())).willReturn(List.of("스터", "터지"));
        given(storeRepository.findNearbyStoresByKeywordOrderedByScore(
                any(), anyList(), anyLong(), anyList(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());
        given(storeRepository.findNearbyStoresForFuzzyMatching(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(List.of(closeName, farName));

        List<StoreResponse> result = storeService.searchStoresInMap(
                "스터지", LAT, LNG, MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlaceName()).isEqualTo("스터디");
    }

    // --- 헬퍼 ---

    private Store storeWithName(Long id, String name) {
        String searchName = name.replaceAll("\\s+", "").toLowerCase();
        return Store.builder()
                .storeId(id)
                .placeName(name)
                .placeNameSearch(searchName)
                .storeType("일반음식점")
                .x(LNG)
                .y(LAT)
                .reviewCount(0)
                .build();
    }
}
