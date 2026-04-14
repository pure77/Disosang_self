package com.pmh.disosang.map.store.service;

import com.pmh.disosang.map.store.StoreSearchTokenRepository;
import com.pmh.disosang.map.store.entity.StoreSearchToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * store_search_token 생성/재생성 전용 서비스
 *
 * 책임:
 * 1. 매장명 정규화 검색 컬럼(place_name_search) 기준 token 생성
 * 2. 검색어를 query token 으로 분해
 *
 * 왜 별도 서비스로 분리하는가?
 * - token 생성 규칙과 검색어 token 생성 규칙이 반드시 같아야 합니다.
 * - 이 규칙이 여러 곳에 흩어지면 저장 시점과 검색 시점 정규화가 달라져 검색 품질이 깨집니다.
 */
@Service
@RequiredArgsConstructor
public class StoreSearchTokenService {

    /**
     * 현재는 매장명만 token 화합니다.
     * 주소까지 같이 넣으면 row 수가 급격히 늘고 noise도 커질 수 있으므로
     * 1차 도입은 PLACE에 한정합니다.
     */
    public static final String FIELD_TYPE_PLACE = "PLACE";

    /**
     * 2-gram token 크기
     * 예:
     * - '치킨' -> ['치킨']
     * - '스타벅스' -> ['스타', '타벅', '벅스']
     */
    private static final int TOKEN_SIZE = 2;

    private final StoreSearchTokenRepository storeSearchTokenRepository;

    /**
     * 특정 매장의 place_name_search token 을 전부 재구축합니다.
     *
     * 동작 순서:
     * 1. 기존 token 삭제
     * 2. 새 2-gram token 생성
     * 3. token 저장
     *
     * 삭제 후 재생성 방식을 쓰는 이유:
     * - 구현이 단순하고
     * - 매장명이 바뀐 경우 stale token 이 남지 않기 때문입니다.
     */
    @Transactional
    public void rebuildPlaceNameTokens(Long storeId, String placeNameSearch) {
        storeSearchTokenRepository.deleteByStoreIdAndFieldType(storeId, FIELD_TYPE_PLACE);

        List<StoreSearchToken> tokens = buildPlaceTokens(storeId, placeNameSearch);
        if (!tokens.isEmpty()) {
            storeSearchTokenRepository.saveAll(tokens);
        }
    }

    /**
     * 검색어를 2-gram query token 으로 분해합니다.
     *
     * 예:
     * - '치킨' -> ['치킨']
     * - '스터디' -> ['스터', '터디']
     *
     * LinkedHashSet 을 쓰는 이유:
     * - 같은 token 이 반복될 수 있는데,
     *   DISTINCT 기준 집계와 minMatchCount 계산을 할 때 중복이 오히려 왜곡을 만들 수 있기 때문입니다.
     * - 순서는 유지하되 중복 token 은 제거합니다.
     */
    public List<String> createQueryTokens(String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.length() < TOKEN_SIZE) {
            return List.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        for (int i = 0; i <= normalizedKeyword.length() - TOKEN_SIZE; i++) {
            tokens.add(normalizedKeyword.substring(i, i + TOKEN_SIZE));
        }

        return new ArrayList<>(tokens);
    }

    /**
     * place_name_search 문자열을 실제 DB에 저장할 token row 목록으로 만듭니다.
     */
    private List<StoreSearchToken> buildPlaceTokens(Long storeId, String text) {
        if (text == null || text.length() < TOKEN_SIZE) {
            return List.of();
        }

        List<StoreSearchToken> tokens = new ArrayList<>();

        /**
         * 문자열을 왼쪽부터 한 글자씩 이동시키며 2글자 token 을 생성합니다.
         * pos 는 사람이 보기 쉽도록 1-based 로 저장합니다.
         */
        for (int i = 0; i <= text.length() - TOKEN_SIZE; i++) {
            tokens.add(StoreSearchToken.builder()
                    .storeId(storeId)
                    .fieldType(FIELD_TYPE_PLACE)
                    .token(text.substring(i, i + TOKEN_SIZE))
                    .tokenLen(TOKEN_SIZE)
                    .pos(i + 1)
                    .build());
        }

        return tokens;
    }
}
