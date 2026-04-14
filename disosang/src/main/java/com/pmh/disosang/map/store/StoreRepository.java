package com.pmh.disosang.map.store;

import com.pmh.disosang.map.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByPlaceName(String placeName);
    List<Store> findByXIsNull();
    Optional<Store> findByStoreId(Long storeId);

    @Query(value = """
        /*
         * nearby
         * - 먼저 현재 지도 화면(bounds) 안에 들어오는 매장만 추립니다.
         * - exact 검색도 전체 store를 보지 않고 nearby 후보에 대해서만 수행해 비용을 줄입니다.
         */
        WITH nearby AS (
            SELECT /*+ MATERIALIZATION */
                   s.store_id, s.lon, s.lat
            FROM store s FORCE INDEX (spx_store_location)
            WHERE MBRContains(
                ST_GeomFromText(
                    CONCAT(
                        'POLYGON((',
                        :minLng, ' ', :minLat, ',',
                        :maxLng, ' ', :minLat, ',',
                        :maxLng, ' ', :maxLat, ',',
                        :minLng, ' ', :maxLat, ',',
                        :minLng, ' ', :minLat,
                        '))'
                    ),
                    4326,
                    'axis-order=long-lat'
                ),
                s.location
            )
        )
        SELECT s.*
        FROM nearby n
        JOIN store s ON s.store_id = n.store_id
        WHERE s.place_name_search = :qSearch
        ORDER BY ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat)), s.store_id
    """, nativeQuery = true)
    List<Store> findExactNameStoresOrderedByDistance(
            @Param("qSearch") String qSearch,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    @Query(value = """
        /*
         * global exact
         * - 현재 지도 안에 exact 결과가 없을 때 전체 범위에서 exact 이름을 찾습니다.
         * - 여전히 place_name_search 인덱스를 타므로 비용이 낮습니다.
         */
        SELECT s.*
        FROM store s FORCE INDEX (idx_store_place_search)
        WHERE s.place_name_search = :qSearch
        ORDER BY ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat)), s.store_id
    """, nativeQuery = true)
    List<Store> findExactNameStoresGloballyOrderedByDistance(
            @Param("qSearch") String qSearch,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng
    );

    @Query(value = """
        /*
         * short keyword query
         * - 1글자 검색은 token 검색까지 열면 잡음이 너무 커질 수 있으므로
         *   기존 prefix 중심 전략을 유지합니다.
         */
        WITH nearby AS (
            SELECT /*+ MATERIALIZATION */
                   s.store_id, s.lon, s.lat
            FROM store s FORCE INDEX (spx_store_location)
            WHERE MBRContains(
                ST_GeomFromText(
                    CONCAT(
                        'POLYGON((',
                        :minLng, ' ', :minLat, ',',
                        :maxLng, ' ', :minLat, ',',
                        :maxLng, ' ', :maxLat, ',',
                        :minLng, ' ', :maxLat, ',',
                        :minLng, ' ', :minLat,
                        '))'
                    ),
                    4326,
                    'axis-order=long-lat'
                ),
                s.location
            )
        ),
        hits AS (
            /*
             * 매장명 prefix
             * - 1글자 검색에서는 가장 안전한 검색 신호입니다.
             */
            SELECT s.store_id, 120 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_place_search) ON s.store_id = n.store_id
            WHERE s.place_name_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            /*
             * 업종 prefix
             */
            SELECT s.store_id, 40 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_store_type_search) ON s.store_id = n.store_id
            WHERE s.store_type_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            /*
             * 카테고리 매칭
             */
            SELECT s.store_id, 20 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_category_id) ON s.store_id = n.store_id
            WHERE s.category_id IN (:categoryIds)
        ),
        best_hit AS (
            SELECT store_id, MAX(score) AS score
            FROM hits
            GROUP BY store_id
        )
        SELECT s.*
        FROM best_hit b
        JOIN store s ON s.store_id = b.store_id
        ORDER BY
            b.score DESC,
            ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat)),
            s.store_id
        LIMIT 50
    """, nativeQuery = true)
    List<Store> findNearbyStoresByShortKeywordOrderedByScore(
            @Param("qSearch") String qSearch,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    @Query(value = """
        /*
         * nearby
         * - 먼저 현재 지도 화면(bounds) 안에 들어오는 매장만 추립니다.
         * - 이후 exact / prefix / token / category / address 검색은 모두 이 후보 집합에 대해서만 수행됩니다.
         */
        WITH nearby AS (
            SELECT /*+ MATERIALIZATION */
                   s.store_id, s.lon, s.lat
            FROM store s FORCE INDEX (spx_store_location)
            WHERE MBRContains(
                ST_GeomFromText(
                    CONCAT(
                        'POLYGON((',
                        :minLng, ' ', :minLat, ',',
                        :maxLng, ' ', :minLat, ',',
                        :maxLng, ' ', :maxLat, ',',
                        :minLng, ' ', :maxLat, ',',
                        :minLng, ' ', :minLat,
                        '))'
                    ),
                    4326,
                    'axis-order=long-lat'
                ),
                s.location
            )
        ),

        /*
         * token_hits
         * - query token 과 store_search_token 이 얼마나 겹치는지 계산합니다.
         * - 기존 LOCATE() 기반 infix와 다르게 token 인덱스를 탈 수 있어 부하 상황에서 더 안정적입니다.
         *
         * 계산값:
         * - matched_token_count:
         *   검색어 token 중 몇 개가 이 매장과 겹쳤는지
         * - min_pos:
         *   가장 앞쪽에서 매칭된 token 위치
         */
        token_hits AS (
            SELECT
                t.store_id,
                COUNT(DISTINCT t.token) AS matched_token_count,
                MIN(t.pos) AS min_pos
            FROM store_search_token t
            JOIN nearby n ON n.store_id = t.store_id
            WHERE t.field_type = 'PLACE'
              AND t.token IN (:queryTokens)
            GROUP BY t.store_id
            HAVING COUNT(DISTINCT t.token) >= :minTokenMatchCount
        ),

        /*
         * hits
         * - exact / prefix / token / type / category / address 신호를 모두 누적합니다.
         * - 하나의 매장이 여러 규칙에 동시에 걸릴 수 있으므로 UNION ALL 로 모읍니다.
         *
         * 컬럼 의미:
         * - score:
         *   검색 강도
         * - match_pos:
         *   앞쪽 매칭 우선순위를 비교하는 위치 값
         * - match_type:
         *   어떤 규칙에 의해 결과가 올라왔는지 확인하기 위한 디버깅용 값
         */
        hits AS (
            /*
             * 매장명 exact
             * - 가장 강한 신호
             */
            SELECT s.store_id, 1000 AS score, 0 AS match_pos, 'PLACE_EXACT' AS match_type
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_place_search) ON s.store_id = n.store_id
            WHERE s.place_name_search = :qSearch

            UNION ALL

            /*
             * 매장명 prefix
             * - 사용자가 기대하는 기본 검색 패턴이라 token보다 높은 점수를 유지합니다.
             */
            SELECT s.store_id, 350 AS score, 1 AS match_pos, 'PLACE_PREFIX' AS match_type
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_place_search) ON s.store_id = n.store_id
            WHERE s.place_name_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            /*
             * token 기반 부분검색
             * - fallback 이 아니라 항상 같이 검색합니다.
             * - 이유:
             *   '치킨' 검색 시 prefix 매장이 많아도 가까운 '청년치킨' 같은 suffix/infix 매장을
             *   후보군에서 놓치지 않기 위해서입니다.
             */
            SELECT th.store_id,
                   (120 + (th.matched_token_count * 40)) AS score,
                   th.min_pos AS match_pos,
                   'PLACE_TOKEN' AS match_type
            FROM token_hits th

            UNION ALL

            /*
             * 업종 exact
             */
            SELECT s.store_id, 90 AS score, 0 AS match_pos, 'TYPE_EXACT' AS match_type
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_store_type_search) ON s.store_id = n.store_id
            WHERE s.store_type_search = :qSearch

            UNION ALL

            /*
             * 업종 prefix
             */
            SELECT s.store_id, 45 AS score, 1 AS match_pos, 'TYPE_PREFIX' AS match_type
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_store_type_search) ON s.store_id = n.store_id
            WHERE s.store_type_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            /*
             * 카테고리 매칭
             * - 위치 개념이 없으므로 match_pos 는 큰 값으로 둡니다.
             */
            SELECT s.store_id, 80 AS score, 9999 AS match_pos, 'CATEGORY' AS match_type
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_category_id) ON s.store_id = n.store_id
            WHERE s.category_id IN (:categoryIds)

            UNION ALL

            /*
             * 도로명 주소 prefix
             * - 주소는 이름보다 noise가 크므로 낮은 보조 점수만 부여합니다.
             */
            SELECT s.store_id, 25 AS score, 1 AS match_pos, 'ROAD_PREFIX' AS match_type
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_road_address_search) ON s.store_id = n.store_id
            WHERE s.road_address_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            /*
             * 지번 주소 prefix
             */
            SELECT s.store_id, 18 AS score, 1 AS match_pos, 'ADDR_PREFIX' AS match_type
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_address_search) ON s.store_id = n.store_id
            WHERE s.address_search LIKE CONCAT(:qSearch, '%')
        ),

        /*
         * ranked_hits
         * - 하나의 매장이 여러 규칙에 동시에 걸릴 수 있으므로
         *   매장별로 가장 좋은 hit 하나만 남깁니다.
         */
        ranked_hits AS (
            SELECT
                h.*,
                ROW_NUMBER() OVER (
                    PARTITION BY h.store_id
                    ORDER BY h.score DESC, h.match_pos ASC
                ) AS rn
            FROM hits h
        )

        /*
         * 최종 SQL 후보 추출
         * - 이 결과는 사용자에게 바로 보여주는 최종 순위가 아니라
         *   Java 재정렬용 후보 집합입니다.
         */
        SELECT s.*
        FROM ranked_hits h
        JOIN store s ON s.store_id = h.store_id
        WHERE h.rn = 1
        ORDER BY
            h.score DESC,
            h.match_pos ASC,
            s.review_count DESC,
            ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat)),
            s.store_id
        LIMIT 150
    """, nativeQuery = true)
    List<Store> findNearbyStoresByKeywordOrderedByScore(
            @Param("qSearch") String qSearch,
            @Param("queryTokens") List<String> queryTokens,
            @Param("minTokenMatchCount") long minTokenMatchCount,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );
}
