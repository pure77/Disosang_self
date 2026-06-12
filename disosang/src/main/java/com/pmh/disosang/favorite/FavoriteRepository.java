package com.pmh.disosang.favorite;

import com.pmh.disosang.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUser_IdAndStore_StoreId(Long userId, Long storeId);

    Optional<Favorite> findByUser_IdAndStore_StoreId(Long userId, Long storeId);

    @Query("select f.store.storeId from Favorite f where f.user.id = :userId")
    List<Long> findStoreIdsByUserId(@Param("userId") Long userId);

    /*
     * 홈 즐겨찾기 목록용
     * - store를 fetch join 으로 함께 조회해 N+1을 방지합니다.
     */
    @Query("select f from Favorite f join fetch f.store s left join fetch s.category where f.user.id = :userId order by f.createdAt desc")
    List<Favorite> findByUserIdWithStore(@Param("userId") Long userId);
}
