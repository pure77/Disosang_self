package com.pmh.disosang.map.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장명 부분검색(infix / suffix)을 위한 2-gram token 엔티티
 *
 * 역할:
 * - 저장 시점에 place_name_search를 2글자씩 잘라 token row로 저장합니다.
 * - 검색 시점에는 LOCATE()로 문자열 전체를 훑는 대신,
 *   token = '치킨' 같은 인덱스 가능한 조회로 후보를 찾습니다.
 *
 * 예:
 * - place_name_search = '청년치킨'
 * - token row = '청년', '년치', '치킨'
 */
@Entity
@Table(name = "store_search_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(StoreSearchTokenId.class)
public class StoreSearchToken {

    /**
     * 어떤 매장의 token 인지 식별합니다.
     */
    @Id
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 어떤 필드를 token 화한 것인지 구분합니다.
     * 현재는 PLACE만 먼저 사용합니다.
     */
    @Id
    @Column(name = "field_type", nullable = false, length = 20)
    private String fieldType;

    /**
     * 실제 2-gram token 문자열
     * 예: '청년치킨' -> '청년', '년치', '치킨'
     */
    @Id
    @Column(name = "token", nullable = false, length = 20)
    private String token;

    /**
     * token 이 원문 문자열에서 시작한 위치(1-based)
     * 이후 Java 재정렬에서 앞쪽 매칭 가산점 계산에 사용할 수 있습니다.
     */
    @Id
    @Column(name = "pos", nullable = false)
    private Integer pos;

    /**
     * token 길이
     * 현재는 2-gram만 사용하므로 2가 저장됩니다.
     */
    @Column(name = "token_len", nullable = false)
    private Integer tokenLen;
}
