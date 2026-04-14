package com.pmh.disosang.map.store.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * store_search_token 복합키 클래스
 *
 * 복합키 구성:
 * - store_id
 * - field_type
 * - token
 * - pos
 *
 * pos를 키에 넣는 이유:
 * - 같은 토큰이 같은 매장명 안에서 여러 번 등장할 수 있기 때문입니다.
 * - 예를 들어 반복 문자열에서는 token 자체만으로는 row를 유일하게 식별할 수 없습니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StoreSearchTokenId implements Serializable {

    private Long storeId;
    private String fieldType;
    private String token;
    private Integer pos;
}
