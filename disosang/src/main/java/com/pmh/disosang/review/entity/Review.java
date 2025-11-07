package com.pmh.disosang.review.entity;

import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review") // DB 테이블 이름
@Getter
@Setter
@NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(nullable = false)
    private Integer rating;

    private String content;

    // Review(N)가 Store(1)에 속함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // Review(N)가 User(1)에 속함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // (User 엔티티 필요)

    // Review(1)가 Photo(N)를 가짐
    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Photo> photos = new ArrayList<>();

    public void update(String newContent, int newRating) {
        this.content = newContent;
        this.rating = newRating;
    }
    // ... (Getter, Setter, 생성자 등)
}