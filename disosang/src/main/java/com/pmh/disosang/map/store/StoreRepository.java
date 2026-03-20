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
        SELECT s.store_id
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
    """, nativeQuery = true)
    List<Long> findNearbyStoreIds(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    @Query(value = """
        SELECT s.*
        FROM store s
        WHERE s.store_id IN (:nearbyStoreIds)
          AND s.category_id IN (:categoryIds)
        ORDER BY ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat)), s.store_id
    """, nativeQuery = true)
    List<Store> findNearbyStoresByCategoryIdsOrderedByDistance(
            @Param("nearbyStoreIds") List<Long> nearbyStoreIds,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng
    );

    @Query(value = """
        SELECT s.*
        FROM store s
        WHERE s.store_id IN (:nearbyStoreIds)
          AND MATCH (
              s.place_name,
              s.address_name,
              s.road_address_name,
              s.store_type
          ) AGAINST (:keyword IN NATURAL LANGUAGE MODE)
        ORDER BY ST_Distance_Sphere(point(:centerLng, :centerLat), point(s.lon, s.lat)), s.store_id
    """, nativeQuery = true)
    List<Store> findNearbyStoresByKeywordOrderedByDistance(
            @Param("nearbyStoreIds") List<Long> nearbyStoreIds,
            @Param("keyword") String keyword,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng
    );
}
