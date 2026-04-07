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

    private static final int SHORT_KEYWORD_LENGTH = 2;
    private static final int FUZZY_CANDIDATE_LIMIT = 150;
    private static final String STORE_TYPE_CHEAP = "cheap";
    private static final String STORE_TYPE_TRADITIONAL_MARKET = "tm";

    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;

    public List<StoreResponse> searchStoresInMap(String keyword, double centerLat, double centerLng,
                                                 double minLat, double maxLat,
                                                 double minLng, double maxLng) {

        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        boolean storeTypeAliasKeyword = isStoreTypeAliasKeyword(normalizedKeyword);
        String compactKeyword = normalizeSearchKeyword(normalizedKeyword);

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
                return exactStores.stream()
                        .map(StoreResponse::fromEntity)
                        .collect(Collectors.toList());
            }

            List<Store> globalExactStores = storeRepository.findExactNameStoresGloballyOrderedByDistance(
                    compactKeyword,
                    centerLat,
                    centerLng
            );
            if (!globalExactStores.isEmpty()) {
                return globalExactStores.stream()
                        .map(StoreResponse::fromEntity)
                        .collect(Collectors.toList());
            }
        }

        List<Long> categoryIds = storeTypeAliasKeyword
                ? List.of(-1L)
                : toSearchableCategoryIds(getExpandedCategoryIds(normalizedKeyword));
        List<Store> stores;

        if (compactKeyword.length() <= SHORT_KEYWORD_LENGTH) {
            stores = storeRepository.findNearbyStoresByShortKeywordOrderedByScore(
                    compactKeyword,
                    categoryIds,
                    centerLat,
                    centerLng,
                    minLat,
                    maxLat,
                    minLng,
                    maxLng
            );
        } else {
            stores = storeRepository.findNearbyStoresByKeywordOrderedByScore(
                    compactKeyword,
                    categoryIds,
                    centerLat,
                    centerLng,
                    minLat,
                    maxLat,
                    minLng,
                    maxLng
            );
        }

        if (stores.isEmpty() && compactKeyword.length() >= 3 && !storeTypeAliasKeyword) {
            stores = findFuzzyMatches(compactKeyword, centerLat, centerLng, minLat, maxLat, minLng, maxLng);
        }

        return stores.stream()
                .map(StoreResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ");
    }

    private String compactKeyword(String keyword) {
        return keyword == null ? "" : keyword.replaceAll("\\s+", "").toLowerCase();
    }

    private String normalizeSearchKeyword(String keyword) {
        String compact = compactKeyword(keyword);

        return switch (compact) {
            case "착한가격업소", "착한가격" -> STORE_TYPE_CHEAP;
            case "전통시장" -> STORE_TYPE_TRADITIONAL_MARKET;
            default -> compact;
        };
    }

    private boolean isStoreTypeAliasKeyword(String keyword) {
        String compact = compactKeyword(keyword);

        return compact.equals("착한가격업소")
                || compact.equals("착한가격")
                || compact.equals("전통시장");
    }

    private List<Long> toSearchableCategoryIds(List<Long> categoryIds) {
        return categoryIds.isEmpty() ? List.of(-1L) : categoryIds;
    }

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

    private List<Store> findFuzzyMatches(String compactKeyword, double centerLat, double centerLng,
                                         double minLat, double maxLat, double minLng, double maxLng) {
        int maxDistance = Math.max(1, Math.min(2, compactKeyword.length() / 3));

        return storeRepository.findNearbyStoresForFuzzyMatching(
                        centerLat,
                        centerLng,
                        minLat,
                        maxLat,
                        minLng,
                        maxLng,
                        FUZZY_CANDIDATE_LIMIT
                ).stream()
                .filter(store -> isFuzzyCandidate(store, compactKeyword, maxDistance))
                .sorted(Comparator
                        .comparingInt((Store store) -> levenshteinDistance(compactKeyword, compactKeyword(store.getPlaceName())))
                        .thenComparing(Store::getStoreId))
                .limit(10)
                .collect(Collectors.toList());
    }

    private boolean isFuzzyCandidate(Store store, String compactKeyword, int maxDistance) {
        String compactName = compactKeyword(store.getPlaceName());
        if (compactName.isBlank()) {
            return false;
        }

        if (Math.abs(compactName.length() - compactKeyword.length()) > maxDistance) {
            return false;
        }

        return levenshteinDistance(compactKeyword, compactName) <= maxDistance;
    }

    private int levenshteinDistance(String source, String target) {
        int[][] dp = new int[source.length() + 1][target.length() + 1];

        for (int i = 0; i <= source.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= target.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= source.length(); i++) {
            for (int j = 1; j <= target.length(); j++) {
                int substitutionCost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + substitutionCost
                );
            }
        }

        return dp[source.length()][target.length()];
    }

    @Transactional(readOnly = true)
    public StoreResponse findById(long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("ID 를 찾을 수 없습니다."));

        return StoreResponse.fromEntity(store);
    }
}
