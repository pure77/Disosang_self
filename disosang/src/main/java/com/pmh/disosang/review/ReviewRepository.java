package com.pmh.disosang.review;

import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.review.entity.Review;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review,Long > {
    // Store 객체를 받아 리뷰를 최신순으로 정렬
    List<Review> findByStoreOrderByReviewIdDesc(Store store);

    @Query("SELECT r FROM Review r JOIN FETCH r.store s JOIN FETCH r.user u LEFT JOIN FETCH r.photos p WHERE s = :store")
    List<Review> findReviewsByStoreWithFetchJoin(@Param("store") Store store, Sort sort);

}
