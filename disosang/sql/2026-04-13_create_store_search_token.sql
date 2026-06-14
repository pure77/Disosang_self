/*
 * store_search_token
 * - 매장명 부분검색(infix / suffix)을 빠르게 처리하기 위한 token 인덱스 테이블
 *
 * 왜 필요한가?
 * - 기존 LOCATE(:qSearch, place_name_search)는 문자열 전체를 훑기 때문에 인덱스를 잘 타지 못합니다.
 * - 사용자 수가 늘어나면 p95 응답시간이 크게 튈 수 있습니다.
 * - 이를 해결하기 위해 저장 시점에 place_name_search를 2-gram으로 잘라 별도 테이블에 저장합니다.
 *
 * 예:
 * - place_name_search = '청년치킨'
 * - 저장 token = '청년', '년치', '치킨'
 *
 * 그러면 검색 시 token='치킨' 인덱스를 타고 후보를 바로 찾을 수 있습니다.
 */
CREATE TABLE store_search_token (
    store_id BIGINT NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    token VARCHAR(20) NOT NULL,
    token_len TINYINT NOT NULL,
    pos SMALLINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    /*
     * 복합 PK를 두는 이유:
     * - 같은 store / field / token / pos 조합은 하나만 존재해야 합니다.
     * - pos를 포함하는 이유는 같은 token이 한 문자열 안에 여러 번 반복될 수 있기 때문입니다.
     */
    PRIMARY KEY (store_id, field_type, token, pos),

    /*
     * token 기반 후보 조회용 인덱스
     * - 검색어 token 으로 매장 후보를 찾을 때 사용됩니다.
     */
    KEY idx_store_search_token_lookup (token, field_type, store_id),

    /*
     * 특정 매장 token 삭제/재생성용 인덱스
     * - insert/update 시 해당 매장 token 을 갈아끼울 때 사용됩니다.
     */
    KEY idx_store_search_token_store (store_id, field_type),

    CONSTRAINT fk_store_search_token_store
        FOREIGN KEY (store_id) REFERENCES store(store_id)
        ON DELETE CASCADE
);
