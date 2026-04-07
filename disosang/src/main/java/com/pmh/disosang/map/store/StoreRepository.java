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
            SELECT s.store_id, 120 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_place_search) ON s.store_id = n.store_id
            WHERE s.place_name_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            SELECT s.store_id, 40 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_store_type_search) ON s.store_id = n.store_id
            WHERE s.store_type_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

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
            SELECT s.store_id, 1000 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_place_search) ON s.store_id = n.store_id
            WHERE s.place_name_search = :qSearch

            UNION ALL

            SELECT s.store_id, 350 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_place_search) ON s.store_id = n.store_id
            WHERE s.place_name_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            SELECT s.store_id, 90 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_store_type_search) ON s.store_id = n.store_id
            WHERE s.store_type_search = :qSearch

            UNION ALL

            SELECT s.store_id, 45 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_store_type_search) ON s.store_id = n.store_id
            WHERE s.store_type_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            SELECT s.store_id, 80 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_category_id) ON s.store_id = n.store_id
            WHERE s.category_id IN (:categoryIds)

            UNION ALL

            SELECT s.store_id, 25 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_road_address_search) ON s.store_id = n.store_id
            WHERE s.road_address_search LIKE CONCAT(:qSearch, '%')

            UNION ALL

            SELECT s.store_id, 18 AS score
            FROM nearby n
            JOIN store s FORCE INDEX (idx_store_address_search) ON s.store_id = n.store_id
            WHERE s.address_search LIKE CONCAT(:qSearch, '%')
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
            s.review_count DESC,
            ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat)),
            s.store_id
        LIMIT 50
    """, nativeQuery = true)
    List<Store> findNearbyStoresByKeywordOrderedByScore(
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
        ORDER BY ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat)), s.store_id
        LIMIT :limitCount
    """, nativeQuery = true)
    List<Store> findNearbyStoresForFuzzyMatching(
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("limitCount") int limitCount
    );
}
