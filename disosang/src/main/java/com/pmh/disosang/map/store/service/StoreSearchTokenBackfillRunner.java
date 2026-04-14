package com.pmh.disosang.map.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * store_search_token 초기 적재를 "명시적으로" 실행하기 위한 Runner
 *
 * 핵심 원칙:
 * - 평소 서버 실행 시에는 아무 것도 하지 않습니다.
 * - --backfill-store-search-token 인자가 있을 때만 전체 backfill 을 실행합니다.
 *
 * 왜 이런 구조인가?
 * - 초기 적재는 무거운 작업이므로 서버가 켜질 때마다 자동 실행되면 위험합니다.
 * - 운영자가 필요할 때만 직접 명령으로 1회 실행하는 편이 가장 안전합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreSearchTokenBackfillRunner implements ApplicationRunner {

    /**
     * 애플리케이션 실행 시 이 옵션이 있을 때만 backfill 을 수행합니다.
     *
     * 예:
     * --backfill-store-search-token
     */
    private static final String BACKFILL_OPTION = "backfill-store-search-token";

    private final StoreSearchTokenBatchService storeSearchTokenBatchService;

    @Override
    public void run(ApplicationArguments args) {
        /*
         * 실제로 어떤 실행 옵션이 들어왔는지 먼저 남깁니다.
         * 옵션 전달이 안 된 경우와 배치 로직 자체 문제를 구분하는 데 필요합니다.
         */
        log.info("Store search token runner args: {}", args.getOptionNames());

        /**
         * 평소 서버 실행은 여기서 바로 종료됩니다.
         * 즉 "자동 실행"이 아니라 "명시적으로 요청된 경우에만 1회 실행"되는 구조입니다.
         */
        if (!args.containsOption(BACKFILL_OPTION)) {
            log.info("store_search_token backfill skipped because option '--{}' was not provided", BACKFILL_OPTION);
            return;
        }

        log.info("store_search_token backfill started");
        storeSearchTokenBatchService.rebuildAllStoreSearchTokens();
        log.info("store_search_token backfill completed");
    }
}
