package com.pmh.disosang.review;

import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review,Long > {
    // Store 객체를 받아 리뷰를 최신순으로 정렬
    List<Review> findByStoreOrderByReviewIdDesc(Store store);

}
