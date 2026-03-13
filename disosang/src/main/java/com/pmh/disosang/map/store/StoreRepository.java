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
        SELECT s.*
        FROM store s
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
        AND s.category_id IN (:categoryIds)
        ORDER BY ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat))
    """, nativeQuery = true)
    List<Store> findStoresInAreaByCategoryIdsOrderedByDistance(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    @Query(value = """
        SELECT s.*
        FROM store s
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
        AND MATCH (
            s.place_name,
            s.address_name,
            s.road_address_name,
            s.store_type
        ) AGAINST (:keyword IN NATURAL LANGUAGE MODE)
        ORDER BY ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat))
    """, nativeQuery = true)
    List<Store> findStoresInAreaByKeywordOrderedByDistance(
            @Param("keyword") String keyword,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    @Query(value = """
        SELECT s.*
        FROM store s
        WHERE MATCH (
            s.place_name,
            s.address_name,
            s.road_address_name,
            s.store_type
        ) AGAINST (:keyword IN NATURAL LANGUAGE MODE)
        ORDER BY ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat))
    """, nativeQuery = true)
    List<Store> findStoresByKeyword(
            @Param("keyword") String keyword,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng
    );
}
