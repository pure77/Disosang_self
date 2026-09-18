# Spring Boot 4.1 전환 계획

작성일: 2026-09-18
상태: 0단계 진행 중
전제: LTS 업그레이드 완료 상태 (Java 25, Gradle 9.7.1, Boot 3.5.16, MySQL 8.4.11 Docker, Flyway). 계획: `2026-09-16-stack-lts-upgrade.md`

---

## 1. 왜 지금, 왜 4.1

- Boot 3.5 OSS 지원 **2026-06-30 종료**. 3.5.16(06-25)이 마지막 OSS 패치. 이후 보안 패치는 상용 지원만.
- 4.0은 2026-12-31 OSS 종료 → 곧 같은 문제 반복. **4.1**은 2027-07-31까지, Java 17~26.
- 4.0을 거치지 않고 4.1로 바로 간다. 4.0→4.1 변경(릴리스 노트 확인)은 이 프로젝트에 닿는 항목이 없다.

## 2. 버전 목표 (Maven Central 2026-09-18 확인)

| 구성 요소 | 현재 (3.5.16 BOM) | 목표 (4.1.1 BOM) |
|---|---|---|
| Spring Boot | 3.5.16 | **4.1.1** |
| Spring Framework | 6.2.19 | 7.0.9 |
| Spring Security | 6.5.11 | 7.1.1 |
| Spring Data | 2025.0.x | 2026.0.1 |
| Hibernate ORM | 6.6.53 | 7.4.5 |
| Jackson | 2.19 (`com.fasterxml`) | **3.1.5 (`tools.jackson`)** |
| Tomcat | 10.1.55 | 11.0.24 (Servlet 6.1) |
| Flyway | 11.7.2 | **12.4.0** |
| H2 (테스트) | 2.3.232 | 2.4.240 |
| Mockito | 5.17 | 5.23 |
| Thymeleaf | 3.1.3 | 3.1.5 |
| Lombok, Connector/J | 1.18.46, 9.7.0 | 동일 |
| springdoc-openapi | 2.8.5 | **3.1.1** (Boot 4 라인) |

## 3. 영향 지점 조사 결과 (0단계, 코드 46파일 grep + 공식 마이그레이션 가이드)

### 확실히 바꿔야 하는 것
| 항목 | 현재 | 4.1에서 | 근거 |
|---|---|---|---|
| 웹 스타터 | `spring-boot-starter-web` | `spring-boot-starter-webmvc` (구 이름은 deprecated) | 가이드 "Deprecated Starters" |
| Flyway | `flyway-core` + `flyway-mysql` 직접 | `spring-boot-starter-flyway` | 가이드 "Main Code": 서드파티 직접 의존은 스타터로 교체 필요 |
| springdoc | 2.8.5 | 3.1.1 | 2.x는 Boot 3/Jackson 2 전용 |
| 테스트 스타터 | `spring-boot-starter-test` | 유지 가능. `@SpringBootTest` 1개에 MockMvc 미사용이라 추가 스타터 불필요 (실패 시 `spring-boot-starter-webmvc-test` 추가) | 가이드 "Test Code" |
| 설정 프로퍼티 | — | 전환 중 `spring-boot-properties-migrator`(runtimeOnly)로 이름 바뀐 프로퍼티 검출, 끝나면 제거 | 가이드 "Configuration Properties Migration" |

### 영향 없음으로 확인된 것
| 항목 | 확인 내용 |
|---|---|
| Jackson import | `com.fasterxml.jackson` 직접 import **0건**. 직렬화 기본값 변화만 3단계에서 응답 diff로 확인 |
| `@MockBean`/`@SpyBean` | 0건. 테스트는 `MockitoExtension` + `@Mock`/`@InjectMocks` (가이드가 권하는 방식) |
| Security DSL | 이미 람다 DSL (`authorizeHttpRequests`, `formLogin(...)`, `rememberMe(...)`, `logout(...)`). 체이닝 `.and()` 0건, `AntPathRequestMatcher` 0건 |
| Spring Data 제거 API | `getById`/`getOne`/`findOne` 0건. `Sort.by`만 사용 (유지) |
| `javax.*` 잔재 | 0건 |
| Hibernate 직접 사용 | `@ColumnDefault` 1건 (Hibernate 7 유지) |
| JSpecify null 검사 | 빌드에 null 체커 없음 → 컴파일 영향 없음 |
| `RestTemplate` | `StoreBatchService` 1건. Framework 7에서 유지 (제거 아님) |
| `HttpHeaders` | 주석 처리된 코드 1건뿐 |
| Servlet API 직접 사용 | 0건 |

### 3단계(동작 점검)에서 볼 것
- Jackson 3 기본값: 날짜 직렬화 형식, null 필드 처리, `Double` 표기 → 검색 API 응답을 3.5.16 vs 4.1로 diff
- Security 7 기본값: CSRF 토큰 처리, 기본 보안 헤더, rememberMe → 브라우저로 로그인·리뷰 작성·찜 확인
- Hibernate 7: 네이티브 쿼리 5개 결과 타입, `validate` 엄격도 → 기동 시 validate 통과 + 검색 결과 건수 비교
- Flyway 12: 기존 `flyway_schema_history`(V1 baseline) 호환 → 기동 시 "up to date" 확인
- Thymeleaf 템플릿: 화면 6종 수동 확인

## 4. 단계

### 0단계. 사전 조사·기준점 (PR 1개: 린트 켜기, 0.5일) — 진행 중
- [x] 공식 4.0 마이그레이션 가이드, 4.1 릴리스 노트 확인
- [x] 코드 grep으로 영향 지점 목록 (§3)
- [x] `build.gradle`에 `-Xlint:deprecation` 추가 (영구, 제거 대상 검출용)
- [x] `-Xlint:deprecation` 빌드 결과: **컴파일 경고 0건** (main + test). 3.5에서 deprecated API 미사용 → 1단계 생략
- [x] 기준점 k6 3회 (master dd246d8 jar, JDK 25, Docker MySQL 8.4): p95 **76 / 70 / 81ms → 중앙값 76ms**, 73 req/s(k6 상한), 실패 0 → `load_test/baseline/2026-09-18_boot41-baseline_{1,2,3}.*`
  - 전날 같은 코드가 54ms였으므로 하루 사이 노이즈가 40%. 서버가 포화 상태가 아니라 절대값이 작을수록 편차 비율이 커진다 → 판정 기준을 §5처럼 조정
- [x] `git tag pre-boot41` → dd246d8, origin 푸시

### 1단계. 3.5.16에서 미리 바꿀 수 있는 것 — **생략** (0단계 결과 deprecation 0건)
- 원래 의도: deprecation 경고 나는 코드를 3.5에서 먼저 새 API로 바꿔 2단계 diff를 줄이는 것
- `-Xlint:deprecation` 빌드에서 경고가 하나도 없어 할 일이 없음. 스타터 이름 변경(`webmvc`, `starter-flyway`)은 4.1에만 존재하므로 2단계에서 처리

### 2단계. Boot 4.1.1 전환 (PR 1개, 1~2일)
1. `build.gradle`: 플러그인 4.1.1, `starter-web`→`starter-webmvc`, Flyway→`starter-flyway`, springdoc 3.1.1, `spring-boot-properties-migrator` runtimeOnly(임시)
2. 컴파일 → 오류 해결 (예상: 적음. §3 기준)
3. 테스트 통과
4. 기동: properties-migrator 경고 확인 → yml 수정, Flyway 12 "up to date", validate 통과, Swagger UI 열림
5. k6 3회
6. properties-migrator 제거 후 최종 커밋
- 막히면: `spring-boot-starter-classic`/`-test-classic`으로 먼저 올린 뒤 스타터를 정리하는 2단 전략 (가이드 "Migration Strategy")

### 3단계. 동작 점검 (PR 0~1개, 0.5일)
- §3 "3단계에서 볼 것" 항목 실행. 차이가 있으면 수정 PR

### 4단계. 문서 (PR 1개, 0.25일)
- README 배지 Boot 4.1, 업그레이드 요약에 한 줄, 이 파일 완료 처리

## 5. 판정·롤백·원칙
- 각 PR: 빌드·테스트 통과, 앱 기동 후 검색 스모크
- k6는 단계마다 돌리지 않고 **전환이 끝난 뒤(3단계 동작 점검 후) 1회(3번 실행)만** 측정한다 — 2026-09-18 사용자 결정, 측정 시간 절약. 회귀가 나오면 그때 2단계 diff를 거꾸로 좁힌다
- **k6 판정 (기준점 76ms 기준)**: 서버가 포화 상태가 아니라 p95 절대값이 작고 하루 노이즈가 ±40%라 +15% 규칙은 쓰지 않는다.
  - 통과: p95 중앙값 ≤ **150ms** (기준점 2배) **이고** 처리량 ≥ 70 req/s(k6 상한 유지) **이고** 실패 0
  - 조사: 150ms 초과 또는 처리량 하락(서버가 다시 병목이 됐다는 뜻) → 원인 분리 후 결정
  - 근거: 실제 회귀(예: Jackson 3 직렬화 비용, Hibernate 7 쿼리 계획 변화)는 p95가 100ms대로 튀거나 처리량이 떨어지는 형태로 나타난다. 76→90ms 수준의 변화는 이 장비에서 구분 불가
- 롤백: 전부 코드 변경 → PR revert. DB 스키마 불변 (Flyway 이력 그대로)
- 기간 중 V2 마이그레이션·검색 품질 작업 금지
- 측정 조건: LTS 계획 §3와 동일 (jar 실행, JDK 25, Docker MySQL 8.4, 워밍업 100회, k6 3회)

## 6. 견적
| 단계 | 예상 | 실제 |
|---|---|---|
| 0. 조사·기준점 | 0.5일 | |
| 1. 3.5에서 선행 정리 | 0.5일 | |
| 2. 4.1 전환 | 1~2일 | |
| 3. 동작 점검 | 0.5일 | |
| 4. 문서 | 0.25일 | |
| 예비 | 1일 | |
| 합계 | 4~5일 | |

## 7. 로그
| 단계 | 날짜 | k6 p95 | 확인 | 문제와 해결 | 예상→실제 |
|---|---|---|---|---|---|
| 기준점 | 2026-09-18 | **76ms** (76 / 70 / 81, avg 33/30/45, 73 req/s, 실패 0) | 태그 pre-boot41 = dd246d8, Docker MySQL 8.4, JDK 25 | 전날 54ms와 40% 차이 → 판정 기준을 절대값(≤150ms)+처리량으로 변경 | 0.5일 → |
| 1 | — | — | 생략 (deprecation 0건) | — | 0.5일 → 0 |
| 2 | 2026-09-18 | **57ms** (57 / 54 / 60, avg 26/25/27, 73 req/s, 실패 0) — 기준점 76ms 대비 회귀 없음, 판정 통과 | 플러그인 4.1.1·`starter-webmvc`·`starter-flyway`·springdoc 3.1.1, **컴파일 오류 0·코드 수정 0**, 10 tests 통과. migrator가 설정 키 5개 검출 → `spring.servlet.encoding.*`, `spring.web.error.include-*`로 변경 후 경고 0. 기동: Flyway 12 이력 그대로(baseline 1), validate 통과, Jackson 3 응답 JSON 3.5와 동일(필드·null·소수 표기), 오타 fallback·오류 message·Swagger UI·로그인 페이지·보안 헤더 OK | 사용자 빌드가 두 번 실행되지 않아(폴더 위치) 확인 지연. `@SpringBootTest`가 dev 프로필로 실제 3307 MySQL에 붙어 도는 것을 발견(기존 동작, 후속 과제) | 1~2일 → 0.5일 |
| 3 | 2026-09-18 | | curl 기준 API·Swagger·보안 헤더 점검 완료. 브라우저 수동(로그인·리뷰 작성·찜) 은 사용자 확인 | | 0.5일 → 0.25일 |
