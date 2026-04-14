package com.pmh.disosang.map.store;

import com.pmh.disosang.map.store.entity.StoreSearchToken;
import com.pmh.disosang.map.store.entity.StoreSearchTokenId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreSearchTokenRepository extends JpaRepository<StoreSearchToken, StoreSearchTokenId> {

    /**
     * 특정 매장의 특정 field_type token 을 전부 삭제합니다.
     *
     * 사용 시점:
     * - 초기 backfill 재실행
     * - 매장명 변경 후 token 재생성
     *
     * "삭제 후 재생성" 전략을 쓰면 데이터 정합성을 단순하게 유지할 수 있습니다.
     */
    void deleteByStoreIdAndFieldType(Long storeId, String fieldType);
}
