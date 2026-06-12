package com.pmh.disosang.favorite.entity;

import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 즐겨찾기
 * - 한 사용자가 한 가게를 즐겨찾기한 관계를 표현합니다.
 * - (user_id, store_id) 조합은 유일해야 하므로 유니크 제약을 둡니다.
 */
@Entity
@Table(
        name = "favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_user_store",
                columnNames = {"user_id", "store_id"}
        )
)
@Getter
@NoArgsConstructor
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Favorite(User user, Store store) {
        this.user = user;
        this.store = store;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
