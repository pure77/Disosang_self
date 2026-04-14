package com.pmh.disosang.map.store.service;

import com.pmh.disosang.map.store.CategoryRepository;
import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    /*
     * 1글자 검색은 token 검색까지 열면 noise가 과하게 커질 수 있습니다.
     * 따라서 1글자까지만 short query 를 사용하고,
     * 2글자 이상은 token 검색이 포함된 long query 로 보냅니다.
     */
    private static final int SHORT_KEYWORD_LENGTH = 1;

    /*
     * 최종 사용자 응답 개수
     * - DB에서는 후보를 넓게 수집하고,
     *   마지막은 Java에서 이 개수만큼 정리합니다.
     */
    private static final int FINAL_RESULT_LIMIT = 50;

    /*
     * token 점수는 exact/prefix 보다 아래에 두는 것이 중요합니다.
     * token coverage가 높더라도 prefix를 역전하지 않도록 상한을 둡니다.
     */
    private static final int TOKEN_SCORE_MAX = 320;

    private static final String STORE_TYPE_CHEAP = "cheap";
    private static final String STORE_TYPE_TRADITIONAL_MARKET = "tm";

    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final StoreSearchTokenService storeSearchTokenService;

    public List<StoreResponse> searchStoresInMap(String keyword, double centerLat, double centerLng,
                                                 double minLat, double maxLat,
                                                 double minLng, double maxLng) {

        /*
         * 1. 사용자 입력 기본 정규화
         * - trim
         * - 연속 공백 축소
         */
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        /*
         * 2. 업종 별칭 여부 확인
         * - 예: 착한가격업소 -> cheap
         */
        boolean storeTypeAliasKeyword = isStoreTypeAliasKeyword(normalizedKeyword);

        /*
         * 3. 실제 검색에 사용할 정규화 키워드 생성
         * - 공백 제거
         * - 소문자화
         * - 업종 별칭이면 내부 코드값으로 치환
         */
        String compactKeyword = normalizeSearchKeyword(normalizedKeyword);

        /*
         * 4. 업종 별칭 검색이 아니라면 exact 매장명 검색을 먼저 시도합니다.
         * exact는 가장 강한 사용자 의도이므로 가장 먼저 빠르게 처리하는 것이 맞습니다.
         */
        if (!storeTypeAliasKeyword) {
            List<Store> exactStores = storeRepository.findExactNameStoresOrderedByDistance(
                    compactKeyword,
                    centerLat,
                    centerLng,
                    minLat,
                    maxLat,
                    minLng,
                    maxLng
            );
            if (!exactStores.isEmpty()) {
                return toResponses(exactStores);
            }

            /*
             * 현재 지도 안 exact 결과가 없더라도
             * 전체 범위 exact 를 한 번 더 확인합니다.
             */
            List<Store> globalExactStores = storeRepository.findExactNameStoresGloballyOrderedByDistance(
                    compactKeyword,
                    centerLat,
                    centerLng
            );
            if (!globalExactStores.isEmpty()) {
                return toResponses(globalExactStores);
            }
        }

        /*
         * 5. 카테고리 보조 신호 계산
         * - 업종 별칭 검색이면 category 검색은 사용하지 않으므로 더미값(-1) 사용
         * - 일반 검색이면 키워드로 category tree를 확장해 보조 점수로 사용
         */
        List<Long> categoryIds = storeTypeAliasKeyword
                ? List.of(-1L)
                : toSearchableCategoryIds(getExpandedCategoryIds(normalizedKeyword));

        /*
         * 6. query token 생성
         * - token table 검색에 사용할 2-gram query token 목록
         * - 예: '스터디' -> ['스터', '터디']
         */
        List<String> queryTokens = storeSearchTokenService.createQueryTokens(compactKeyword);

        /*
         * 최소 몇 개의 token이 맞아야 후보로 인정할지 계산합니다.
         * - query가 길수록 조금 더 많은 token 일치를 요구합니다.
         */
        long minTokenMatchCount = calculateMinTokenMatchCount(queryTokens);

        /*
         * 7. 1차 DB 후보 추출
         * - 1글자 검색은 short query
         * - 2글자 이상은 exact/prefix/token/category/address 를 함께 모으는 long query
         *
         * 중요한 점:
         * - token 검색은 fallback이 아니라 항상 같이 수행합니다.
         * - 이유:
         *   '치킨' 검색 시 prefix 매장이 많아도 가까운 '청년치킨' 같은 suffix/infix 매장을
         *   후보군에서 놓치지 않기 위해서입니다.
         */
        List<Store> candidates = compactKeyword.length() <= SHORT_KEYWORD_LENGTH
                ? storeRepository.findNearbyStoresByShortKeywordOrderedByScore(
                        compactKeyword,
                        categoryIds,
                        centerLat,
                        centerLng,
                        minLat,
                        maxLat,
                        minLng,
                        maxLng
                )
                : storeRepository.findNearbyStoresByKeywordOrderedByScore(
                        compactKeyword,
                        queryTokens,
                        minTokenMatchCount,
                        categoryIds,
                        centerLat,
                        centerLng,
                        minLat,
                        maxLat,
                        minLng,
                        maxLng
                );

        /*
         * 8. Java 최종 재정렬
         * - DB는 후보를 넓게 모으는 역할
         * - Java는 서비스 정책에 맞는 최종 순서를 정하는 역할
         */
        List<Store> rankedStores = rerankCandidates(
                candidates,
                compactKeyword,
                queryTokens,
                centerLat,
                centerLng
        );

        /*
         * 9. 최종 DTO 변환
         */
        return toResponses(rankedStores);
    }

    /**
     * 기본 정규화
     * - trim
     * - 연속 공백 제거
     */
    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ");
    }

    /**
     * 검색용 compact 문자열 생성
     * - 공백 제거
     * - 소문자화
     */
    private String compactKeyword(String keyword) {
        return keyword == null ? "" : keyword.replaceAll("\\s+", "").toLowerCase();
    }

    /**
     * 검색용 최종 정규화
     * - compact 처리
     * - 업종 별칭 치환
     */
    private String normalizeSearchKeyword(String keyword) {
        String compact = compactKeyword(keyword);

        return switch (compact) {
            case "착한가격업소", "착한가격" -> STORE_TYPE_CHEAP;
            case "전통시장" -> STORE_TYPE_TRADITIONAL_MARKET;
            default -> compact;
        };
    }

    /**
     * 업종 별칭 여부 확인
     */
    private boolean isStoreTypeAliasKeyword(String keyword) {
        String compact = compactKeyword(keyword);

        return compact.equals("착한가격업소")
                || compact.equals("착한가격")
                || compact.equals("전통시장");
    }

    /**
     * 빈 category 리스트를 그대로 IN ()에 넘기지 않기 위한 안전장치
     */
    private List<Long> toSearchableCategoryIds(List<Long> categoryIds) {
        return categoryIds.isEmpty() ? List.of(-1L) : categoryIds;
    }

    /**
     * 사용자가 입력한 키워드로 카테고리를 확장합니다.
     * - exact category name 우선
     * - 없으면 keyword category 검색
     * - 이후 하위 카테고리까지 모두 확장
     */
    private List<Long> getExpandedCategoryIds(String keyword) {
        List<Long> rootCategoryIds = categoryRepository.findCategoryIdsByExactName(keyword);
        if (rootCategoryIds.isEmpty()) {
            rootCategoryIds = categoryRepository.findCategoryIdsByKeyword(keyword);
        }

        Set<Long> categoryIds = new LinkedHashSet<>();
        for (Long rootId : rootCategoryIds) {
            categoryIds.addAll(categoryRepository.findAllDescendantIds(rootId));
        }

        return new ArrayList<>(categoryIds);
    }

    /**
     * query token 최소 일치 개수 계산
     *
     * 정책:
     * - token이 0개면 1
     * - token이 1개면 1
     * - token이 여러 개면 약 60% 이상 맞아야 후보로 인정
     *
     * 너무 낮으면 noise가 많아지고,
     * 너무 높으면 recall이 급격히 떨어집니다.
     */
    private long calculateMinTokenMatchCount(List<String> queryTokens) {
        if (queryTokens.isEmpty()) {
            return 1;
        }
        if (queryTokens.size() == 1) {
            return 1;
        }
        return Math.max(1, (long) Math.ceil(queryTokens.size() * 0.6));
    }

    /**
     * 최종 재정렬
     *
     * 정렬 우선순위:
     * 1. computeSearchScore()가 높은 순
     * 2. review_count가 높은 순
     * 3. 현재 지도 중심과 가까운 순
     * 4. store_id가 작은 순
     *
     * token 검색은 후보를 잘 끌어오지만,
     * 최종 순위는 서비스 정책대로 다시 잡아야 사용자가 기대하는 결과와 맞습니다.
     */
    private List<Store> rerankCandidates(List<Store> candidates,
                                         String compactKeyword,
                                         List<String> queryTokens,
                                         double centerLat,
                                         double centerLng) {
        return candidates.stream()
                .sorted(Comparator
                        .comparingInt((Store store) -> computeSearchScore(store, compactKeyword, queryTokens)).reversed()
                        .thenComparing(Comparator.comparingInt((Store store) ->
                                store.getReviewCount() == null ? 0 : store.getReviewCount()).reversed())
                        .thenComparingDouble(store -> distanceMeters(centerLat, centerLng, store.getY(), store.getX()))
                        .thenComparing(Store::getStoreId))
                .limit(FINAL_RESULT_LIMIT)
                .collect(Collectors.toList());
    }

    /**
     * 최종 검색 점수 계산
     *
     * 우선순위:
     * - exact
     * - prefix
     * - token coverage
     * - type
     * - address
     *
     * token은 infix/suffix 후보를 끌어오는 역할이지만,
     * exact/prefix보다 위로 올라가면 사용자가 기대하는 결과와 어긋날 수 있으므로
     * 상한을 두어 prefix를 넘지 않도록 합니다.
     */
    private int computeSearchScore(Store store, String compactKeyword, List<String> queryTokens) {
        String place = readSearchValue(store.getPlaceNameSearch(), store.getPlaceName());
        if (place.equals(compactKeyword)) {
            return 1000;
        }

        if (place.startsWith(compactKeyword)) {
            return 350;
        }

        int tokenCoverageScore = computeTokenCoverageScore(place, queryTokens);
        if (tokenCoverageScore > 0) {
            return Math.min(TOKEN_SCORE_MAX, 120 + tokenCoverageScore);
        }

        String type = readSearchValue(store.getStoreTypeSearch(), store.getStoreType());
        if (type.equals(compactKeyword)) {
            return 90;
        }
        if (type.startsWith(compactKeyword)) {
            return 45;
        }

        String road = readSearchValue(store.getRoadAddressSearch(), store.getRoadAddressName());
        if (road.startsWith(compactKeyword)) {
            return 25;
        }

        String addr = readSearchValue(store.getAddressSearch(), store.getAddressName());
        if (addr.startsWith(compactKeyword)) {
            return 18;
        }

        return 0;
    }

    /**
     * token coverage 점수 계산
     *
     * 무엇을 보나?
     * - query token 중 몇 개가 매장명에 포함되는가
     * - 가장 앞쪽에서 매칭된 token 위치는 어디인가
     * - query token 전체가 다 맞았는가
     *
     * 예:
     * - query: '스터디' -> ['스터', '터디']
     * - place: 'ㅇㅇ스터디카페' -> 2개 모두 매칭, 높은 점수
     * - place: 'ㅇㅇ스터카페' -> 1개만 매칭, 낮은 점수
     */
    private int computeTokenCoverageScore(String place, List<String> queryTokens) {
        if (queryTokens.isEmpty()) {
            return 0;
        }

        int matched = 0;
        int minPos = Integer.MAX_VALUE;

        for (String token : queryTokens) {
            int pos = place.indexOf(token);
            if (pos >= 0) {
                matched++;
                minPos = Math.min(minPos, pos + 1);
            }
        }

        if (matched == 0) {
            return 0;
        }

        /*
         * matched 개수가 많을수록 점수 증가
         */
        int coverageScore = matched * 40;

        /*
         * 앞쪽에서 매칭될수록 가산점
         */
        int positionScore = minPos == Integer.MAX_VALUE ? 0 : Math.max(0, 30 - (minPos * 3));

        /*
         * query token 전체가 다 맞으면 보너스
         */
        int fullCoverageBonus = matched == queryTokens.size() ? 60 : 0;

        return coverageScore + positionScore + fullCoverageBonus;
    }

    /**
     * DB 정규화 검색 컬럼이 있으면 우선 사용하고,
     * null이면 원본 문자열을 같은 규칙으로 compact 처리해 fallback 합니다.
     */
    private String readSearchValue(String searchValue, String rawValue) {
        if (searchValue != null && !searchValue.isBlank()) {
            return searchValue;
        }
        return compactKeyword(rawValue);
    }

    /**
     * 두 좌표 사이의 직선 거리(미터)를 계산합니다.
     * Java 재정렬에서 동점자 비교 기준으로 사용합니다.
     */
    private double distanceMeters(double centerLat, double centerLng, Double lat, Double lng) {
        if (lat == null || lng == null) {
            return Double.MAX_VALUE;
        }

        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat - centerLat);
        double dLng = Math.toRadians(lng - centerLng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(centerLat))
                * Math.cos(Math.toRadians(lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * 엔티티 -> 응답 DTO 변환
     */
    private List<StoreResponse> toResponses(List<Store> stores) {
        return stores.stream()
                .limit(FINAL_RESULT_LIMIT)
                .map(StoreResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreResponse findById(long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("ID 를 찾을 수 없습니다."));

        return StoreResponse.fromEntity(store);
    }
}
