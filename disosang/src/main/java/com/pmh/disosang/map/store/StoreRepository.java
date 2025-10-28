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

    @Query("""
SELECT DISTINCT s FROM Store s
WHERE s.x IS NOT NULL AND s.y IS NOT NULL
  AND s.x BETWEEN :minLng AND :maxLng
  AND s.y BETWEEN :minLat AND :maxLat
  AND (
      s.placeName LIKE %:keyword%
   OR s.category LIKE %:keyword%
   OR s.addressName LIKE %:keyword%
   OR s.roadAddressName LIKE %:keyword%
   OR s.storeType LIKE %:keyword%
  )
ORDER BY function('ST_Distance_Sphere', function('point', :centerLng, :centerLat), function('point', s.x, s.y))
""")
    List<Store> findStoresInAreaByKeywordOrderedByDistance(
            @Param("keyword") String keyword,
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    @Query("""
SELECT s FROM Store s
WHERE s.x IS NOT NULL AND s.y IS NOT NULL
  AND (
      s.placeName LIKE %:keyword%
   OR s.category LIKE %:keyword%
   OR s.addressName LIKE %:keyword%
   OR s.roadAddressName LIKE %:keyword%
   OR s.storeType LIKE %:keyword%
  )
ORDER BY function('ST_Distance_Sphere', function('point', :centerLng, :centerLat), function('point', s.x, s.y))
""")
    List<Store> findStoresByKeyword(@Param("keyword") String keyword, @Param("centerLat") double centerLat, @Param("centerLng") double centerLng);
}
