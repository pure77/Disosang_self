package com.pmh.disosang.map.store.service;

import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.entity.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * store_search_token 초기 적재(backfill)와 개별 동기화용 서비스
 *
 * 1+2 적재 방식:
 * 1. 초기 도입 시 전체 store를 순회하며 1회 backfill
 * 2. 이후 매장 insert/update 시 해당 매장 token 만 동기 갱신
 *
 * 주의:
 * - 현재 코드베이스에는 "매장명 자체를 수정하는 활성화된 저장 흐름"이 뚜렷하지 않습니다.
 * - 리뷰 저장처럼 review_count만 바꾸는 store save에도 token 재생성을 묶어버리면 낭비가 큽니다.
 * - 그래서 여기서는 개별 동기화 메서드까지 준비하되,
 *   실제 store insert/update 지점에서만 명시적으로 호출하는 구조를 권장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreSearchTokenBatchService {

    private final StoreRepository storeRepository;
    private final StoreSearchTokenService storeSearchTokenService;

    /**
     * 기존 store 전체를 순회하면서 token 을 재생성합니다.
     *
     * 사용 의도:
     * - token table 최초 도입 시 1회 수동 실행
     * - 필요 시 운영자가 명시적으로 재적재
     */
    @Transactional
    public void rebuildAllStoreSearchTokens() {
        List<Store> stores = storeRepository.findAll();
        log.info("store_search_token backfill target store count: {}", stores.size());

        int processed = 0;
        int skipped = 0;

        for (Store store : stores) {
            if (store.getPlaceNameSearch() == null || store.getPlaceNameSearch().isBlank()) {
                skipped++;
                log.warn(
                        "Skipping store token rebuild because placeNameSearch is empty. storeId={}, placeName='{}', placeNameSearch='{}'",
                        store.getStoreId(),
                        store.getPlaceName(),
                        store.getPlaceNameSearch()
                );
                continue;
            }

            log.info(
                    "Rebuilding store token. storeId={}, placeName='{}', placeNameSearch='{}'",
                    store.getStoreId(),
                    store.getPlaceName(),
                    store.getPlaceNameSearch()
            );
            rebuildStoreSearchTokens(store);
            processed++;
        }

        log.info(
                "store_search_token backfill summary: processed={}, skipped={}",
                processed,
                skipped
        );
    }

    /**
     * 특정 매장 1건에 대한 token 동기화
     *
     * 이후 store insert/update 시점에 이 메서드를 호출하면
     * 2번 요구사항인 "이후 동기 갱신"을 충족할 수 있습니다.
     */
    @Transactional
    public void rebuildStoreSearchTokens(Store store) {
        storeSearchTokenService.rebuildPlaceNameTokens(
                store.getStoreId(),
                store.getPlaceNameSearch()
        );
    }
}
