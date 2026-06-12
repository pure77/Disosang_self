/*
 * favorite
 * - 사용자가 가게를 즐겨찾기한 관계를 저장하는 테이블입니다.
 *
 * 왜 필요한가?
 * - 상세페이지에서 즐겨찾기 토글, 홈 화면의 즐겨찾기 목록, 지도의 노란 별 핀 표시에 사용됩니다.
 *
 * 정책:
 * - (user_id, store_id) 조합은 한 번만 존재해야 하므로 유니크 제약을 둡니다.
 * - 회원/가게가 삭제되면 즐겨찾기도 함께 정리되도록 ON DELETE CASCADE 를 둡니다.
 */
CREATE TABLE favorite (
    favorite_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    store_id    BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (favorite_id),

    CONSTRAINT uk_favorite_user_store UNIQUE (user_id, store_id),

    KEY idx_favorite_user (user_id),

    CONSTRAINT fk_favorite_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_favorite_store
        FOREIGN KEY (store_id) REFERENCES store(store_id)
        ON DELETE CASCADE
);
