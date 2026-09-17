-- =====================================================================
-- V1__baseline.sql
-- 2026-09-17 기준 tetto_test(MySQL 8.0.36)의 실제 스키마를 그대로 옮긴 베이스라인.
--
-- 포함: 애플리케이션 엔티티가 매핑하거나(category, users, store, store_search_token,
--       review, photo, favorite) README가 설계의 일부로 설명하는(category_mapping) 테이블.
-- 제외: 해커톤 결제 기능 잔재(payment, paystore, point_wallet, point_transaction)와
--       데이터 정리용 작업 테이블(store_no_location). 코드 참조 0건, 결제 테이블은 행 0건.
--
-- 주의: 아래 세 가지는 JPA 엔티티에 없거나 읽기 전용이라 ddl-auto로는 만들어지지 않는다.
--   1) store.location (POINT, SRID 4326, NOT NULL) + SPATIAL INDEX spx_store_location
--   2) store.road_address_search / address_search (STORED 생성 컬럼)
--   3) store.place_name_search / store_type_search (일반 컬럼, 앱이 쓰지 않음 → 적재 시 채워야 함)
-- 이 파일이 그것들을 보장한다. 기존 DB에는 baseline-on-migrate로 "적용됨" 처리되고,
-- 빈 DB(테스트 컨테이너, 배포 환경)에서는 실제로 실행된다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- category: 표준 카테고리 트리 (parent_id 자기 참조)
-- ---------------------------------------------------------------------
CREATE TABLE `category` (
  `id`         bigint      NOT NULL AUTO_INCREMENT,
  `name`       varchar(50) NOT NULL,
  `parent_id`  bigint      DEFAULT NULL,
  `depth`      int         NOT NULL,
  `group_code` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `category_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- category_mapping: 원본 데이터의 비정형 업종명 → 표준 category id 번역표
-- ---------------------------------------------------------------------
CREATE TABLE `category_mapping` (
  `raw_category` varchar(50) NOT NULL,
  `category_id`  bigint      NOT NULL,
  PRIMARY KEY (`raw_category`),
  KEY `category_id` (`category_id`),
  CONSTRAINT `category_mapping_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
CREATE TABLE `users` (
  `user_id`   bigint       NOT NULL AUTO_INCREMENT,
  `name`      varchar(100) NOT NULL,
  `email`     varchar(100) NOT NULL,
  `password`  varchar(255) NOT NULL,
  `is_active` tinyint(1)   NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `email_2` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- store: 가맹점. 검색 성능의 핵심 컬럼과 인덱스가 모두 여기 있다.
--   location             : POINT SRID 4326, 지도 영역(MBR) 검색용 공간 인덱스 대상
--   place_name_search    : 매장명 정규화(소문자·공백제거). 일반 컬럼, 적재 시 채움
--   road_address_search  : 도로명 주소 정규화. STORED 생성 컬럼
--   address_search       : 지번 주소 정규화. STORED 생성 컬럼
--   store_type_search    : 업종 정규화. 일반 컬럼, 적재 시 채움
--   category / x / y     : 원본 CSV 시절 잔재 컬럼(엔티티 미매핑). 현 상태 유지
-- ---------------------------------------------------------------------
CREATE TABLE `store` (
  `store_id`            bigint       NOT NULL AUTO_INCREMENT,
  `place_name`          varchar(100) NOT NULL,
  `address_name`        varchar(255) DEFAULT NULL,
  `road_address_name`   varchar(255) DEFAULT NULL,
  `phone`               varchar(20)  DEFAULT NULL,
  `lon`                 double       DEFAULT NULL,
  `lat`                 double       DEFAULT NULL,
  `created_at`          datetime     DEFAULT CURRENT_TIMESTAMP,
  `store_type`          varchar(255) NOT NULL,
  `average_rating`      double       DEFAULT '0',
  `review_count`        int          DEFAULT '0',
  `thumbnail_url`       varchar(255) DEFAULT NULL,
  `category_id`         bigint       DEFAULT NULL,
  `location`            point        NOT NULL SRID 4326,
  `place_name_search`   varchar(255) DEFAULT NULL,
  `road_address_search` varchar(255) GENERATED ALWAYS AS (REPLACE(LOWER(TRIM(IFNULL(`road_address_name`, ''))), ' ', '')) STORED,
  `address_search`      varchar(255) GENERATED ALWAYS AS (REPLACE(LOWER(TRIM(IFNULL(`address_name`, ''))), ' ', '')) STORED,
  `store_type_search`   varchar(255) DEFAULT NULL,
  `category`            varchar(100) DEFAULT NULL,
  `x`                   double       DEFAULT NULL,
  `y`                   double       DEFAULT NULL,
  PRIMARY KEY (`store_id`),
  KEY `idx_store_category_id` (`category_id`),
  SPATIAL KEY `spx_store_location` (`location`),
  KEY `idx_store_place_search` (`place_name_search`),
  KEY `idx_store_road_address_search` (`road_address_search`(120)),
  KEY `idx_store_address_search` (`address_search`(120)),
  KEY `idx_store_store_type_search` (`store_type_search`),
  CONSTRAINT `fk_store_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- store_search_token: 매장명 2-gram 역색인 (부분일치 검색용)
-- ---------------------------------------------------------------------
CREATE TABLE `store_search_token` (
  `store_id`   bigint      NOT NULL,
  `field_type` varchar(20) NOT NULL,
  `token`      varchar(20) NOT NULL,
  `token_len`  int         NOT NULL,
  `pos`        int         NOT NULL,
  `created_at` timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`store_id`, `field_type`, `token`, `pos`),
  KEY `idx_store_search_token_lookup` (`token`, `field_type`, `store_id`),
  KEY `idx_store_search_token_store` (`store_id`, `field_type`),
  CONSTRAINT `fk_store_search_token_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- review
-- ---------------------------------------------------------------------
CREATE TABLE `review` (
  `review_id`  bigint       NOT NULL AUTO_INCREMENT,
  `store_id`   bigint       NOT NULL,
  `user_id`    bigint       NOT NULL,
  `rating`     int          NOT NULL,
  `content`    varchar(255) DEFAULT NULL,
  `created_at` datetime(6)  DEFAULT NULL,
  `updated_at` datetime(6)  DEFAULT NULL,
  PRIMARY KEY (`review_id`),
  KEY `store_id` (`store_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `review_ibfk_1` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
  CONSTRAINT `review_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- photo: 리뷰 사진
-- ---------------------------------------------------------------------
CREATE TABLE `photo` (
  `photo_id`   bigint       NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6)  DEFAULT NULL,
  `file_name`  varchar(255) DEFAULT NULL,
  `file_url`   varchar(255) NOT NULL,
  `review_id`  bigint       NOT NULL,
  `store_id`   bigint       NOT NULL,
  PRIMARY KEY (`photo_id`),
  KEY `FKnx36dfpxbxmxifyiq7yvhiohn` (`review_id`),
  KEY `FK5ltky5vhmoj5t3odnjtccnefo` (`store_id`),
  CONSTRAINT `FK5ltky5vhmoj5t3odnjtccnefo` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
  CONSTRAINT `FKnx36dfpxbxmxifyiq7yvhiohn` FOREIGN KEY (`review_id`) REFERENCES `review` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- favorite: 찜 (user × store 유일)
-- ---------------------------------------------------------------------
CREATE TABLE `favorite` (
  `favorite_id` bigint      NOT NULL AUTO_INCREMENT,
  `created_at`  datetime(6) NOT NULL,
  `store_id`    bigint      NOT NULL,
  `user_id`     bigint      NOT NULL,
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `uk_favorite_user_store` (`user_id`, `store_id`),
  KEY `FK6y53o6nmqrfl8maq55updnf01` (`store_id`),
  CONSTRAINT `FK6y53o6nmqrfl8maq55updnf01` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`),
  CONSTRAINT `FKa2lwa7bjrnbti5v12mga2et1y` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
