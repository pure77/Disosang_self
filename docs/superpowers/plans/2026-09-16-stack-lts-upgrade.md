# 스택 LTS 업그레이드 계획

작성일: 2026-09-16
상태: 계획 (착수 전)
범위: 로컬 개발 환경만 존재, 배포 전. 프로젝트 마무리 후 1회 배포 예정.

---

## 1. 왜 하는가

이 프로젝트는 현재 지원이 끝났거나 곧 끝나는 버전 위에 있다.
배포 후 한동안 돌아갈 서비스이고 포트폴리오로 읽히는 저장소이므로, 장기 지원(LTS) 버전으로 맞춘다.

동시에 "내 컴퓨터의 MySQL 안에만 있는 스키마"를 코드로 옮긴다.
이걸 하지 않으면 빈 MySQL에 앱을 띄웠을 때 검색이 깨지므로, 배포 자체가 불가능하다.

### 지원 종료일

| 구성 요소 | 현재 | 상태 | 목표 | 목표 버전 지원 |
|---|---|---|---|---|
| Java | 23 (toolchain) / JAVA_HOME 18 | 23은 2025-03 지원 종료 | 25 (LTS) | 2030년 이후 |
| MySQL | 8.0 (Windows 서비스 MySQL80) | 2026-04 확장 지원 종료 | 8.4 (LTS) | 2032-04 |
| Spring Boot | 3.5.5 | 3.5 계열 구 패치 | 3.5.16 (같은 계열 최신) | 3.5 OSS 종료일 확인 후 기입: ____ |
| Gradle | 8.14.3 | Java 25 toolchain은 9.1+ 필요 | 9.x 최신 | - |
| Lombok | BOM 관리 | JDK 25 지원 버전 필요 | 명시 고정 | - |

Boot 3.5 OSS 지원 종료일은 https://spring.io/projects/spring-boot#support 에서 확인해서 위 표에 적는다.
이 날짜가 8단계(Boot 4)의 마감이 된다.

---

## 2. 현재 상태 (2026-09-16 확인)

### 코드
- `disosang/build.gradle`: Boot 3.5.5, dependency-management 1.1.7, toolchain 23, Lombok 버전 미고정
- `disosang/gradle/wrapper/gradle-wrapper.properties`: 8.14.3
- 테스트: H2 인메모리, `ddl-auto: create-drop`. Testcontainers 없음.
  → H2 테스트는 MySQL 전용 문법(FORCE INDEX, 공간 함수, 생성 컬럼)을 검증하지 못한다.
- `StoreRepository.java`: 네이티브 쿼리가 `FORCE INDEX (spx_store_location)`, `FORCE INDEX (idx_store_place_search)` 사용
- `Store.java`: `*_search` 컬럼은 `insertable=false, updatable=false`로 매핑. `location` 컬럼은 엔티티에 없음.
- `disosang/sql/`: 날짜별 수동 SQL 3개 (review updated_at, store_search_token, favorite)
- `application.yml`: `server:` 블록이 `spring:` 아래에 들여쓰기되어 있음 (버그, 최상위여야 함)
- CI 워크플로, Dockerfile, docker-compose 없음

### 로컬 환경
- JAVA_HOME: `C:\Program Files\Java\jdk-18.0.2.1` (toolchain 23과 불일치)
- 설치된 JDK: 17, 18 (Program Files 기준)
- MySQL 8.0: Windows 서비스 `MySQL80`, 포트 3306, DB `tetto_test`
- MySQL Shell 8.0, Workbench 8.0 설치됨
- Docker Desktop 설치됨 (side-by-side 컨테이너에 사용)
- k6 시나리오: `load_test/store-test.js`, 100 VU ramping, 임계값 p95 < 300ms, 에러율 < 1%

### DB에만 존재하고 코드에 없는 것 (핵심 위험)
- `location` 컬럼 (POINT, SRID)
- `spx_store_location` 공간 인덱스
- `place_name_search`, `address_search`, `road_address_search`, `store_type_search` 생성 컬럼
  → `ddl-auto: update`는 이것들을 못 만들거나(인덱스, location) 일반 컬럼으로 잘못 만든다(`*_search`).
- `idx_store_place_search` 인덱스

---

## 3. 원칙

1. **의존성 순서**: Gradle → Boot 패치 → JDK → MySQL → 문서. 앞 단계가 뒷 단계의 전제다.
2. **되돌릴 수 있는 것 먼저**: 코드 변경은 git revert로 1분. MySQL은 in-place가 아니라 side-by-side로 해서 되돌릴 수 있게 만든다.
3. **PR 하나에 변경 하나**: 실패했을 때 원인이 하나뿐이어야 한다. 업그레이드 기간에는 기능 작업을 끼우지 않는다.
4. **기준점 먼저**: 업그레이드 전 k6 수치를 파일로 남긴다. 성능 회귀는 빌드와 테스트로 안 잡힌다.
5. **기록**: 모든 PR 본문에 확인 항목, 수치, 예상 시간 대비 실제 시간을 적는다.

### 공통 완료 기준 (모든 단계 PR에 적용)
- [ ] `./gradlew clean build` 통과 (테스트 포함)
- [ ] dev 프로필로 앱 기동, 지도 영역 검색 API 1회 호출 성공 (FORCE INDEX 경로 확인)
- [ ] k6 3회 실행, p95 중앙값이 2단계 기준점 중앙값(739ms) 대비 **+15% 이내(≤ 850ms)**.
      (+10%로 잡았으나 기준점 3회 자체의 편차가 712~853ms, 중앙값 대비 +15%라 그 안은 노이즈로 본다. 스크립트의 300ms 임계값은 판정에 쓰지 않는다.)
- [ ] PR 본문에 확인 항목 / 수치 / 예상 vs 실제 시간 기록

### k6 측정 조건 (모든 단계 동일하게)
- 앱은 `./gradlew bootJar`로 만든 jar를 `java -jar`로 실행. **IntelliJ 실행이나 `bootRun`은 쓰지 않는다.**
  둘 다 `-XX:TieredStopAtLevel=1`(C1 전용 JIT)을 붙여서 p95가 2~3배 나온다 (2026-09-17 확인: 940ms~1.28s vs jar 실행).
- JDK: 현재 toolchain과 같은 버전 (`C:\Users\ParkMinHyun\.jdks\corretto-23.0.2\bin\java.exe`, 5단계 이후 25)
- 프로필 dev, 로컬 MySQL, 같은 시드 데이터 (store 26,119행, store_search_token 151,571행)
- 워밍업: 검색 API 100회 선호출 후 k6 시작
- k6 v1.5.0, `load_test/store-test.js` 그대로 (100 VU ramping 30s/1m/30s), 3회 연속
- 결과: `load_test/baseline/<날짜>_<단계>_{1,2,3}.{html,json,log}`, p95 중앙값을 §7 표에 기록
- 장비: i7-1165G7 (4C/8T), 16GB, Windows 11. 노트북이라 회차가 갈수록 느려질 수 있음 → 중앙값 사용

---

## 4. 단계

### 0단계. 사전 정리 (PR 2개, 0.5일)

**목적**: 업그레이드와 무관한 것을 먼저 치워서 기준점을 깨끗하게 만든다.

작업
- PR-0a: `application.yml`의 `server:` 블록을 최상위로 이동. 단독 PR.
- PR-0b (문서만): 이 계획 파일에 Boot 3.5 OSS 종료일 기입. 버전이 적힌 곳 목록 확정:
  - `disosang/build.gradle` (Boot, toolchain, Lombok)
  - `disosang/gradle/wrapper/gradle-wrapper.properties`
  - `README.md` 배지 (Java 23, Spring Boot 3.5)
  - JAVA_HOME, IntelliJ Project SDK
  - (배포 시 추가될 것) Dockerfile 베이스 이미지, CI setup-java, compose MySQL 태그

검증: 앱 기동 후 8080 응답, `server.error.include-message`가 실제로 적용되는지 확인.
롤백: revert.

### 1단계. Flyway 도입 (PR 1개, 1일)

**목적**: DB에만 있는 스키마를 코드로 옮긴다. 빈 MySQL에 앱만 띄우면 인덱스까지 생기게 만든다.
배포 가능 여부를 결정하는 단계이고, 6단계 리허설의 전제다.

작업
1. `flyway-core` + `flyway-mysql` 의존성 추가 (Boot BOM이 버전 관리).
2. 현재 `tetto_test`에서 `SHOW CREATE TABLE`로 모든 테이블 정의 추출.
   `disosang/sql/`의 수동 SQL 3개 내용이 반영된 최종 상태인지 확인.
3. `src/main/resources/db/migration/V1__baseline.sql` 작성. 반드시 포함:
   - `location POINT NOT NULL SRID 4326` (실제 SRID는 DB에서 확인)
   - `SPATIAL INDEX spx_store_location (location)`
   - `*_search` 생성 컬럼 4개 (`GENERATED ALWAYS AS (...) STORED` 정의 그대로)
   - `idx_store_place_search`
   - `store_search_token`, `favorite`, review `updated_at`
4. dev 프로필 `ddl-auto: update` → `validate`. prod는 이미 `none`이므로 유지하되 Flyway가 스키마를 담당.
5. 기존 `tetto_test`에는 이미 스키마가 있으므로 `spring.flyway.baseline-on-migrate: true`, `baseline-version: 1`로 V1을 "이미 적용됨"으로 표시.
6. `disosang/sql/` 폴더는 이력용으로 두거나 README에서 Flyway로 안내 후 정리(별도 판단).

검증
- [ ] Docker로 빈 `mysql:8.0` 컨테이너(포트 3308, 임시) 기동 → 앱 연결 → Flyway가 V1 적용
- [ ] `SHOW CREATE TABLE store` 결과를 기존 DB와 diff. 인덱스 이름, 생성 컬럼 식, SRID 동일 확인
- [ ] 기존 `tetto_test`로 앱 기동 시 validate 통과, `flyway_schema_history`에 V1 baseline 기록
- [ ] H2 테스트: test 프로필에서 `spring.flyway.enabled: false`. 기존 테스트 전부 통과

롤백: revert. `tetto_test`에 생긴 `flyway_schema_history` 테이블만 DROP.

주의: `*_search`가 엔티티에 `insertable=false`로 매핑되어 있어 `validate`는 컬럼 존재만 확인한다. 생성 컬럼 여부는 V1 파일이 보장한다.

### 2단계. 기준점 확보 (PR 없음, 0.5일)

**목적**: "업그레이드 전"을 정확히 찍어 둔다.

작업 (2026-09-17 수행)
- `git tag pre-lts-upgrade` → 커밋 2d8b4ef (PR #6 Flyway 머지 커밋), origin에 푸시 완료
- 덤프: `C:\Users\ParkMinHyun\Desktop\Disosang_backup\tetto_test_pre-lts-upgrade_2026-09-17.sql` (15MB, 저장소 밖)
  `mysqldump --single-transaction --routines --triggers --set-gtid-purged=OFF`
- 복원 리허설: Docker Desktop이 꺼져 있어 로컬 MySQL80에 임시 DB `tetto_restore_check`로 복원(22초) → 14개 테이블 행 수 전부 일치, `SHOW CREATE TABLE` 전부 동일, 공간 인덱스 존재·SRID 4326·`FORCE INDEX (spx_store_location)` MBR 질의 정상 → 임시 DB 삭제
- k6 3회 → `load_test/baseline/2026-09-17_pre-upgrade_{1,2,3}.*` (측정 조건은 §3 참고)
  - 참고용으로 남긴 실패 사례: `2026-09-17_ref-intellij-c1only_{1,2,3}.*` — IntelliJ 실행(C1 전용 JIT) 상태에서 p95 940ms / 1.19s / 1.28s. 기준점으로 쓰지 않음.

완료 기준: 태그, 덤프, 복원 확인 로그, k6 3회 파일과 중앙값이 모두 존재.

### 3단계. Gradle 9 (PR 1개, 0.5~1일)

**목적**: Java 25 toolchain을 인식하는 Gradle 확보. 선행 조건 없음.

작업
1. 현재 8.14.3에서 `./gradlew build --warning-mode all` → deprecation 경고 전부 기록
2. 경고 원인 제거 (build.gradle 수정이 생기면 이 PR에 포함)
3. `./gradlew wrapper --gradle-version 9.x` (최신 9.x). Boot 3.5.5 플러그인의 Gradle 9 지원 확인
4. JAVA_HOME 18로 Gradle 9 데몬 실행 가능한지 확인 (Gradle 9는 JDK 17+ 필요, 18이므로 통과)

검증: 공통 완료 기준.
롤백: revert (wrapper 2개 + build.gradle).

### 4단계. Spring Boot 3.5.5 → 3.5.16 (PR 1개, 0.5일)

**목적**: Java 25에서 검증된 라이브러리 조합(Hibernate, ByteBuddy, Mockito, Jackson, Connector/J)을 JDK 변경 전에 확보. 이후 5단계 오류는 JDK 원인으로 확정할 수 있다.

작업: `build.gradle`의 Boot 플러그인 버전만 변경. 코드 수정 없이 통과하는 것이 기대값.
확인: 3.5.16 릴리스 노트에서 Connector/J, Hibernate 버전 변화와 behavior change 항목 읽기.
검증: 공통 완료 기준.
롤백: revert.

### 5단계. JDK 25 (PR 1개, 1일)

**목적**: LTS JDK로 이동. 세 곳(Gradle toolchain, JAVA_HOME, IDE SDK) 불일치 해소.

작업
1. JDK 25 설치 (기존 17, 18 옆에 side-by-side). Temurin 또는 Oracle
2. `build.gradle` toolchain 23 → 25
3. Lombok 버전 명시 고정 (`compileOnly 'org.projectlombok:lombok:1.18.4x'`, JDK 25 지원 버전 확인)
4. JAVA_HOME → JDK 25, IntelliJ Project SDK → 25, Gradle JVM → 25
5. Mockito 동적 에이전트 경고 제거: test task에 `-javaagent`로 mockito-core를 명시 로딩
6. README 배지는 7단계에서 (여기서 건드리지 않음)

검증
- 공통 완료 기준
- `./gradlew build` 로그에 Lombok 오류(`TypeTag`, `UNKNOWN`) 없음
- 테스트 로그에 "dynamically loaded agent" 경고 없음
- 터미널 빌드와 IntelliJ 빌드 결과 동일

롤백: revert + JAVA_HOME 원복.

### 6단계. MySQL 8.4 side-by-side (PR 1개, 1일)

**목적**: DB를 LTS로. 기존 8.0 서비스는 건드리지 않고 8.4 컨테이너를 옆에 띄워 전환한다.
이 과정이 배포 당일 서버에서 할 일과 동일하므로 리허설을 겸한다.

사전 확인
- MySQL 8.4 릴리스 노트: 제거된 변수, 기본값 변경 (mysql_native_password 기본 비활성화, 새 예약어)
- MySQL Shell 8.4 이상 설치 후 `util.checkForServerUpgrade()`를 8.0 서비스에 실행 (현재 Shell 8.0은 8.4 대상 검사 불가)
- 핵심 쿼리 3개(`StoreRepository`의 FORCE INDEX 쿼리)의 `EXPLAIN` 결과를 8.0에서 파일로 저장

작업
1. Docker: `mysql:8.4` 컨테이너, 포트 3307, 새 볼륨. 계정은 처음부터 `caching_sha2_password`로 생성
2. `application-secret.yml`의 url을 3307로 변경 → 앱 기동 → Flyway V1이 스키마 생성
3. 데이터 적재: 2단계 덤프에서 데이터만 로드. 또는 원본 CSV 로더가 있으면 그것으로.
   (어느 방법을 썼는지 §7에 기록. 배포 때 같은 방법을 쓴다)
4. 같은 3개 쿼리 `EXPLAIN`을 8.4에서 저장 → 8.0 결과와 비교. 인덱스 선택이 바뀌었으면 원인 기록
5. 공통 완료 기준 실행 (k6 3회)
6. 통과 시: 문서 단계까지 8.0 서비스 유지. 7단계 완료 후 `MySQL80` 서비스 중지 → 1주 뒤 제거

선택: Testcontainers 도입
- 현재 테스트가 H2라 MySQL 전용 쿼리는 자동 검증이 안 된다.
- `StoreRepository` 네이티브 쿼리에 한해 Testcontainers(`mysql:8.4`) 통합 테스트를 추가하면 이후 업그레이드마다 자동 리허설이 된다.
- 업그레이드 PR과 분리해서 별도 PR로. 이번 범위에서는 선택.

롤백: `application-secret.yml`의 포트를 3306으로 되돌린다. 8.0 서비스는 건드린 적이 없다.

### 7단계. 문서 (PR 1개, 0.5일)

**목적**: README와 실제를 일치시키고 업그레이드 기록을 남긴다.

작업
- README 배지: Java 23 → 25, Spring Boot 3.5 유지, MySQL 8.4 명시, Gradle 9
- README 실행 방법: Flyway로 스키마 자동 생성됨, `disosang/sql/` 수동 실행 불필요 안내
- 이 파일 §7 표를 최종 수치로 채우고 상태를 "완료"로 변경
- 성능 수치 옆에 측정 환경(로컬, CPU, MySQL 버전) 명시

### 8단계. Spring Boot 4 (별도 프로젝트, 기한: Boot 3.5 OSS 종료일)

- 이 계획 범위 밖. Framework 7, Security 7, Hibernate 7, Jackson 3, Tomcat 11이 동시에 바뀌는 메이저 전환.
- Java 25는 이미 끝나 있으므로 그때는 프레임워크만 본다.
- 착수 시 별도 계획 파일 작성.

---

## 5. 소요 시간

| 단계 | 예상 | 실제 |
|---|---|---|
| 0. 사전 정리 | 0.5일 | |
| 1. Flyway | 1일 | |
| 2. 기준점 | 0.5일 | |
| 3. Gradle 9 | 0.5~1일 | |
| 4. Boot 3.5.16 | 0.5일 | |
| 5. JDK 25 | 1일 | |
| 6. MySQL 8.4 | 1일 | |
| 7. 문서 | 0.5일 | |
| 예비 | 1일 | |
| **합계** | **6.5~7일** | |

예비 1일은 3단계(Gradle 메이저)와 5단계(JDK) 중 하나는 예상을 넘긴다고 보고 넣었다.

---

## 6. 배포 시 흐름 (이 계획 완료 후)

1. 서버에 MySQL 8.4 준비 (RDS 또는 컨테이너). 비어 있음.
2. 앱 실행 → Flyway V1 적용 → 테이블, 공간 인덱스, 생성 컬럼 생성
3. 데이터 적재: 6단계에서 쓴 방법 그대로
4. 검색 API 스모크 → k6를 서버 환경에서 3회 실행 → 서버 기준점 신규 기록 (로컬 수치와 비교 대상 아님)
5. 관리형 DB라면 파라미터 그룹의 `sql_mode`, 타임존, 문자셋을 로컬 Docker 기본값과 비교

배포 환경에서 추가되는 버전 고정 지점: Dockerfile 베이스 이미지(`eclipse-temurin:25`), CI setup-java, compose MySQL 태그.

---

## 7. 업그레이드 로그

| 단계 | 날짜 | k6 p95 중앙값 (전 → 후) | 확인 항목 | 문제와 해결 | 예상 → 실제 |
|---|---|---|---|---|---|
| 기준점 | 2026-09-17 | — → **739ms** (3회: 739 / 712 / 853, avg 368/343/364, 실패 0) | 태그 2d8b4ef, 덤프 15MB 복원 22초 14테이블 일치, 공간 인덱스·SRID OK | IntelliJ/bootRun의 C1 전용 JIT로 1차 측정이 940ms~1.28s → jar 실행으로 재측정 | 0.5일 → 0.5일 |
| Gradle 9 | | ___ → ___ | deprecation 0건 | | |
| Boot 3.5.16 | | ___ → ___ | 코드 수정 0 | | |
| JDK 25 | | ___ → ___ | Lombok 고정, agent 경고 0 | | |
| MySQL 8.4 | | ___ → ___ | 체커 통과, EXPLAIN 동일, 인증 OK | | |

---

## 8. 기준점 조사에서 나온 후속 과제 (업그레이드 범위 밖, 완료 후 처리)

### README 434ms와 기준점 739ms의 차이 (2026-09-17 조사)
- 오타 검색(레벤슈타인 fallback)은 04-07 이전부터 있었으나 **04-14 토큰 방식 커밋(0d531b5)이 삭제**했고, **05-20 커밋(41f2d09)이 복구**했다. 434ms는 그 사이(오타 검색 없는 상태)에 측정된 값으로 보이며, 복구 후 재측정 없이 README(06-14)에 옮겨 적힘. 따라서 README의 2차(1.62s, 오타 포함)→3차(434ms, 오타 없음) 개선 폭은 실제보다 크게 적혀 있다.
- k6 키워드 23개 × 6영역 실측: 44%(61/138)가 빈 결과 → 3자 이상이면 fallback 실행.
- A/B (같은 조건, 각 1회):

  | 키워드 집합 | avg | med | p95 | req/s |
  |---|---|---|---|---|
  | 결과 있는 8개만 | 243ms | 175ms | 632ms | 60.2 |
  | 항상 빈 7개만 (fallback 경로) | 519ms | 579ms | 937ms | 49.2 |
  | 원본 23개 (기준점) | 368ms | 382ms | 739ms | 55.0 |

  → fallback 경로가 평균 2.1배 비쌈. 기준점이 434ms보다 높은 주원인.
- 찜 표시(06-12)는 로그인 사용자만 조회하므로 k6(비로그인)에는 영향 없음.

### 과제
1. **빈 결과가 나오면 안 되는 키워드**: `편의점`, `은행`, `신부동 병원`, `천안역 카페`, `신부동 음식점`이 1km 박스에서 항상 빈 결과. 카테고리 매핑이나 주소 검색이 이 키워드를 못 받는 것으로 보임. 검색 품질 문제이면서, 이 키워드들이 오타 fallback까지 타서 비용도 만듦.
2. **오타 fallback 비용**: 지도 안 후보 150개를 거리순 정렬(`ST_Distance_Sphere` ORDER BY, 인덱스 없음) + Java 레벤슈타인. 조건을 좁히거나(예: 토큰 후보가 0일 때만) 후보 수를 줄일 여지.
3. **README 수치 정정**: 3차 최적화 수치 옆에 측정 조건(실행 방식, 날짜, 커밋)을 적고, 현재 코드 기준 수치로 갱신. 필요하면 0d531b5를 jar로 띄워 434ms 재현 여부 확인.
4. **k6 스크립트 개선**: 상태 코드만 확인하므로 빈 결과를 못 잡음. 결과 건수 체크 추가, 결과 경로 env로 지정.
5. **측정 도구 주의**: Git Bash의 curl은 한글 인자를 cp949로 보내므로 UTF-8 퍼센트 인코딩을 직접 만들어야 함. (2단계 워밍업 100회는 이 문제로 실제 검색 경로를 덜 워밍업했음. k6 램프업 30초가 대신 워밍업 역할을 하므로 기준점에는 영향 없다고 판단.)

---

## 9. 참고: 실무 방식과의 대응

| 실무 절차 | 이 프로젝트에서 |
|---|---|
| 릴리스 노트, 업그레이드 체커 | 6단계 사전 확인 |
| 스테이징 리허설 | 6단계 로컬 8.4 컨테이너 |
| 백업 + 복원 확인 | 2단계 |
| 롤백 계획 문서화 | 각 단계 "롤백" 항목 |
| 레플리카 승격 / RDS Blue-Green | 논리 덤프 side-by-side (로컬이라 다운타임 무의미) |
| 실행 계획 전후 비교 | 6단계 EXPLAIN 저장 |
| 이전 인스턴스 유예 후 삭제 | 8.0 서비스 1주 유예 |
| 기록 | §7 표, PR 본문 |
