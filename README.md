<div align="center">

# Disosang — 지역화폐 가맹점 지도 검색

지역화폐(천안사랑카드)로 결제할 수 있는 가게를 **지도 위에서 빠르고 정확하게** 찾아주는 서비스

![thumbnail](docs/images/thumbnail.png)

</div>

---

## 프로젝트 소개

지역화폐는 "내 동네에서 쓸 수 있는 가게"를 찾는 게 사용자의 핵심 목적입니다.
하지만 가맹점이 수만 개라, **지도를 움직일 때마다 빠르게 / 원하는 가게를 정확히** 찾아주지 못하면
사용자는 서비스 자체를 신뢰하지 않게 됩니다.

그래서 Disosang은 **"검색이 곧 서비스의 입구"** 라는 관점에서,
지도 영역 기반 가게 검색의 **속도와 품질**을 끝까지 끌어올리는 데 집중했습니다.

> **이 저장소는 해커톤에서 시작한 프로젝트를, 제가 맡았던 기능을 가져와 단독으로 재구축·개선한 버전입니다.**
> - 해커톤 당시 구현하지 못했거나 아쉬웠던 **지도·가게 검색 기능**을 가져와 직접 개선/재구현했습니다.
> - 프론트엔드는 처음부터 다시 작성했습니다 (**React → Thymeleaf**).
> - 본 서비스 흐름과 무관한 **결제 기능은 제거**하고, 핵심인 **지도·검색·가게 도메인**에 집중했습니다.

---

## 진행 형태 / 기간

| 구분 | 내용                                           |
|------|----------------------------------------------|
| 원본 | 해커톤 팀 프로젝트 (지도·가게 검색 파트 담당)                  |
| 본 저장소 | 담당 기능 단독 재구축 · 검색 엔진 개선                      |
| 담당 범위 | 백엔드 전체 · 프론트(Thymeleaf) 재작성 · DB 설계 · 부하 테스트 |
| 기간 | 팀 : 2025.07 ~ 2025.08 개인 : 2025.09 ~ 현재      |

---

## 주요 기능

- **지도 기반 가게 검색** 
  - 화면에 보이는 영역 안에서 가게를 검색 (공간 인덱스 기반)
  - 정확 일치 · 앞부분 일치 · 부분 일치(중간/끝) · 오타 보정 · 장소 기반 검색("천안역 카페")
  - 검색 결과를 점수·리뷰 수·거리로 재정렬해 가장 관련 있는 가게를 먼저 노출
- **가게 상세 / 지도 포커스** — 상세 정보, 지도에서 해당 가게로 포커스 이동, 길찾기 연동
- **리뷰 / 사진** — 가게별 리뷰 작성·조회, 사진 업로드
- **찜(즐겨찾기)** — 관심 가게 저장/해제
- **카테고리 정규화** — 비정형 업종명을 표준 카테고리 트리로 정리해 "음식점" 같은 상위 검색의 누락 해소
- **회원 / 인증** — 회원가입·로그인 (Spring Security)

---

## 기술 스택

**Backend**  
![Java](https://img.shields.io/badge/Java%2025%20LTS-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.5.16-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle%209.7-02303A?style=flat-square&logo=gradle&logoColor=white)

**Database**  
![MySQL](https://img.shields.io/badge/MySQL%208.4%20LTS-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)

**Frontend**  
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=thymeleaf&logoColor=white)
![Kakao Map](https://img.shields.io/badge/Kakao%20Map-FFCD00?style=flat-square&logo=kakao&logoColor=black)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black)

**Test / Docs**  
![k6](https://img.shields.io/badge/k6-7D64FF?style=flat-square&logo=k6&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black)

---

## 아키텍처

별도 배포 인프라 없이 동작하는 **계층형 모놀리식** 구조입니다.
대신 **DB 안쪽(공간 인덱스 · 토큰 역색인 · 카테고리 트리)** 설계에 깊이를 두었습니다.

<div align="center">

![아키텍처](docs/images/architecture.png)

</div>

> **요청 흐름** — 사용자가 지도를 움직이면 → `View(Kakao Map)` 가 검색 요청 → `Controller → StoreService` 가
> 공간 인덱스로 영역을 좁히고 토큰/이름으로 후보를 모은 뒤 Java에서 최종 랭킹 → 결과를 지도에 렌더.

---

## ERD

핵심 도메인(가게·검색·리뷰·찜·카테고리) 기준 ERD입니다.

<div align="center">

![ERD](docs/images/erd.svg)

</div>

- **`store` ↔ `store_search_token`** — 매장명을 2-gram으로 분해해 저장하는 역색인 (1:N, `ON DELETE CASCADE`)
- **`category` (self-ref)** — `parent_id` 로 표현하는 카테고리 트리(인접목록), `category_mapping` 으로 비정형 업종명을 표준 id로 번역
- **`store`** — `location(POINT, SRID 4326)` 공간 컬럼 + `place_name_search` 등 검색 정규화 컬럼 보유

---

## 핵심 — 지도 가게 검색 엔진 개선

같은 검색 기능을 **3차에 걸쳐** 성능과 품질 양쪽에서 개선했습니다.

| 단계 | 한 일 | 검색 p95 | 트레이드오프 |
|------|-------|---------|------------|
| **1차** | `LIKE '%키워드%'` 풀스캔 → 공간 인덱스 + 정규화 컬럼 prefix | **812ms → 110ms** | 속도 ↑ / 검색이 '앞부분 일치'로 좁아짐 |
| **2차** | 부분일치·오타·장소 검색 추가 | **110ms → 1.62s** | 품질 ↑ / `LOCATE` 풀스캔으로 부하 시 성능 붕괴 |
| **3차** | `LOCATE` → **2-gram 토큰 역색인** 으로 교체 | **1.62s → 712ms** | 부분일치·오타 보정 품질을 **유지하며** 속도 회복 |

> **p95** = 응답의 95%가 이 시간 안에 완료 — 평균과 달리 가장 느린 구간까지 챙기는 지표
>
> 3차 수치는 2026-09-17 재측정값입니다. `java -jar` 실행, 100 VU 램핑 2분, 오타 보정 fallback 포함, 3회 측정(712 / 739 / 853ms) 중 최솟값.
> 이전에 적혀 있던 434ms는 토큰 방식 도입 직후 오타 보정이 빠져 있던 시점의 값이라 정정했습니다. 결과 파일은 `load_test/baseline/`에 있습니다.
>
> **측정 환경 주의** — 위 3단계 수치는 모두 같은 노트북(i7-1165G7, 4C/8T)의 **Windows MySQL 8.0 서비스** 위에서 잰 값입니다.
> 같은 코드를 **Docker MySQL 8.4 (Linux)** 에 붙이면 p95 **54ms** (2026-09-18, 3회 중앙값)입니다.
> 같은 8.0.36이라도 Docker가 Windows 서비스보다 동시 10접속에서 2.4배 빨랐고, 앱 커넥션 풀(10)이 DB 대기에서 풀리면서 대기열이 사라진 결과입니다.
> 배포 환경(Linux)에 가까운 값은 후자이며, 상세는 `load_test/baseline/2026-09-18_db-concurrency-bench.txt`에 있습니다.

**설계 요점**

- **"DB는 후보 추출, Java는 최종 랭킹"** 으로 책임 분리 — SQL은 인덱스 잘 타는 후보 추출만, 자주 바뀌는 랭킹 정책은 테스트 가능한 Java에서
- **직접 만든 2-gram 토큰 역색인** — n-gram FULLTEXT의 noise(`"맑은닭칼국수"`에 `"맑은이비인후과"` 매칭)를 피하려고, 토큰을 "후보 추출"로만 쓰고 임계값·점수상한·Java 재정렬로 품질을 직접 통제
- **인덱스/옵티마이저 레벨 결정** — 공간 인덱스로 먼저 좁히고 `FORCE INDEX` + CTE `MATERIALIZATION`으로 "지역 먼저" 전략을 강제

---

## 실행 방법

빈 MySQL에 앱을 붙이면 **Flyway가 스키마(공간 인덱스·생성 컬럼 포함)를 자동으로 만듭니다.** 수동 DDL 실행은 필요 없습니다.

```powershell
# 1. MySQL 8.4 컨테이너 (Docker Desktop 필요). 루트 비밀번호는 환경 변수로 주입
$env:MYSQL_ROOT_PASSWORD = "<비밀번호>"
docker compose -f docker/mysql84/docker-compose.yml up -d      # 3307 포트

# 2. 로컬 비밀 설정 (git 제외) — disosang/src/main/resources/application-secret.yml
#    spring.datasource.url=jdbc:mysql://localhost:3307/tetto_test, username/password, kakao.maps.js.key

# 3. 빌드 후 jar 실행 (JDK 25)
cd disosang; .\gradlew bootJar
java -jar build\libs\disosang-0.0.1-SNAPSHOT.jar
```

- 가게 데이터는 별도 적재가 필요합니다 (CSV 로더 또는 덤프). 스키마와 데이터를 분리해 두어 배포 환경에서도 같은 순서로 재현합니다.
- 부하 테스트: 앱을 **`java -jar`로 띄운 상태**에서 `k6 run load_test/store-test.js`. IntelliJ 실행이나 `bootRun`은 `-XX:TieredStopAtLevel=1`(C1 전용 JIT)이 붙어 p95가 2~3배 나오므로 측정에 쓰지 않습니다.

---

## 기술 스택 LTS 업그레이드 (2026-09)

지원이 끝난 버전(Java 23, MySQL 8.0)을 장기 지원 버전으로 옮기면서, 배포 가능한 상태(코드만으로 DB 재현)를 함께 만들었습니다.
**PR 하나에 변경 하나**, 되돌릴 수 있는 것부터, 단계마다 같은 조건의 k6 3회로 회귀 확인. 상세 계획과 기록은 [docs/superpowers/plans/2026-09-16-stack-lts-upgrade.md](docs/superpowers/plans/2026-09-16-stack-lts-upgrade.md).

| 단계 | 변경 | 확인 | p95 (3회 중앙값) | PR |
|---|---|---|---|---|
| 0 | `application.yml` 들여쓰기 버그 수정 | SnakeYAML 파싱, 테스트 | — | #4 |
| 1 | **Flyway 도입**, DB에만 있던 스키마를 `V1__baseline.sql`로 코드화 | 임시 DB에 적용 후 8개 테이블 DDL 동일, `ddl-auto: validate` 통과 | — | #6 |
| 2 | 기준점: git tag, 덤프·복원 리허설, k6 3회 | 14개 테이블 행 수·DDL 일치 | **739ms** (Windows MySQL 8.0) | #7 |
| 3 | Gradle 8.14.3 → **9.7.1** | deprecation 0건, 테스트 통과 | 693ms | #8 |
| 4 | Spring Boot 3.5.5 → **3.5.16** | 코드 수정 0, 테스트 통과 | 679ms | #9 |
| 5 | Java 23 → **25 LTS** (Corretto), Lombok 고정, Mockito `-javaagent` | 클래스 버전 69, 동적 에이전트 경고 0 | 807ms | #10 |
| 6 | MySQL 8.0 → **8.4 LTS**, Docker side-by-side | 빈 8.4에 Flyway V1 실행, 데이터 적재 후 행 수·SRID·EXPLAIN 비교 | **54ms** (Docker MySQL 8.4) | #11 |

- 6단계의 13배 개선은 8.4보다 **Windows 서비스 → Linux 컨테이너** 효과가 대부분입니다 (같은 8.0.36 기준 2.4배). 풀 크기를 10 → 20으로 늘려도 Windows에서는 변화가 없어(776ms) DB 처리 용량이 병목이었음을 확인했습니다.
- 조사 중 발견한 사실: 3차 최적화의 이전 수치 434ms는 오타 보정이 빠진 상태의 측정값이라 정정했습니다.

---

## 프로젝트 폴더 구조

<details>
<summary><b>Back-end</b></summary>

```
disosang/src/main/java/com/pmh/disosang/
├── map/store/                      # 지도·가게·검색 (핵심 도메인)
│   ├── controller/                 # StoreController, StoreBatchController
│   ├── service/
│   │   ├── StoreService.java               # 검색 오케스트레이션 + Java 재정렬
│   │   ├── StoreSearchTokenService.java     # 2-gram 토큰 생성/관리
│   │   └── StoreSearchTokenBatchService.java # 토큰 backfill
│   ├── entity/                     # Store, Category, StoreSearchToken
│   ├── StoreRepository.java        # 공간·UNION ALL·토큰 네이티브 쿼리
│   └── dto/
├── review/                         # 리뷰·사진
├── favorite/                       # 찜
├── user/                           # 회원·인증
└── config/                         # Security, Web 설정
```

</details>

<details>
<summary><b>Front-end</b> (Thymeleaf + Vanilla JS)</summary>

```
disosang/src/main/resources/
├── templates/                  # Thymeleaf 화면
│   ├── home/home.html          # 메인(지도) 페이지
│   ├── store/
│   │   ├── map.html            # 지도 가게 검색 화면
│   │   └── detail.html         # 가게 상세
│   ├── user/                   # 로그인 / 회원가입 / 내 정보
│   │   ├── login.html
│   │   ├── signup.html
│   │   └── userInfo.html
│   └── welcome.html
└── static/
    ├── js/
    │   ├── map.js              # 지도·검색 인터랙션 (Kakao Map)
    │   ├── detail.js           # 가게 상세
    │   ├── directions.js       # 길찾기(현위치 기반)
    │   └── signup.js
    └── css/                    # map / detail / signup / userInfo / welcome
```

</details>

<details>
<summary><b>기타</b></summary>

```
disosang/src/main/resources/db/migration/   # Flyway 마이그레이션 (V1__baseline.sql = 현재 스키마 전체)
docker/mysql84/      # 로컬 MySQL 8.4 컨테이너 정의 (docker compose)
disosang/sql/        # Flyway 도입 전 수동 DDL (이력용, 실행 불필요)
load_test/           # k6 부하 테스트 결과 (버전별) · baseline/ = 업그레이드 단계별 측정
docs/                # 설계 해설, 업그레이드 계획 등 문서
```

</details>

---

## 화면 구성

사용자가 **가게를 찾아 → 살펴보고 → 찜하고 → 길을 찾는** 흐름을 따라 구성했습니다.

### 1. 시작 — 랜딩 / 로그인 / 회원가입

| 랜딩 | 로그인 | 회원가입 |
|:---:|:---:|:---:|
| <img src="docs/images/landing.png" width="240"/> | <img src="docs/images/login.png" width="240"/> | <img src="docs/images/signup.png" width="240"/> |

### 2. 지도 검색 

화면에 보이는 영역 안에서 검색하고, 카테고리로 거르고, 핀·목록을 누르면 하단에 가게 정보가 뜹니다.

| 지도 검색 | 카테고리 + 목록 | 가게 선택 |
|:---:|:---:|:---:|
| <img src="docs/images/map-search.png" width="240"/> | <img src="docs/images/search-list.png" width="240"/> | <img src="docs/images/store-popup.png" width="240"/> |
| 키워드로 검색 → 지도 위 핀 + 결과 목록 | 업종 필터 · 페이지네이션 | 핀/목록 클릭 → 미리보기 + 길찾기 |

**착한가격업소**는 지도에서 **초록색 핀**으로 구분해 표시합니다.

| 착한가격업소 (초록 핀) |
|:---:|
| <img src="docs/images/green-pin.png" width="240"/> |
| '착한가격업소' 검색 시 일반 가게(파란 핀)와 색으로 구분 |

### 3. 가게 상세 / 리뷰

| 가게 상세 |                     리뷰 · 사진                     |
|:---:|:-----------------------------------------------:|
| <img src="docs/images/store-detail.png" width="260"/> | <img src="docs/images/review.png" width="260"/> |
| 지도 이동 · 즐겨찾기 · 도착(길찾기) · 후기 |                별점 + 사진 리뷰 작성/조회                 |

### 4. 찜(즐겨찾기)

| 상세에서 찜 | 지도 핀 변경 | 홈 — 찜 목록 |
|:---:|:---:|:---:|
| <img src="docs/images/favorite-detail.png" width="240"/> | <img src="docs/images/favorite-pin.png" width="240"/> | <img src="docs/images/home-favorite.png" width="240"/> |
| 즐겨찾기 ON | 찜한 가게는 지도에서 핀 색 구분 | 홈 탭에 찜한 가게 모음 |

### 5. 길찾기

상세의 **'도착'** 또는 가게목록의 **'갈찾기'** 을 누르면 현위치 기준으로 카카오맵 길찾기로 연결됩니다.

<div align="center">

<img src="docs/images/directions.png" width="720"/>

</div>

### 6. 내 정보

|                       내 정보                       |
|:------------------------------------------------:|
| <img src="docs/images/my-info.png" width="240"/> |
|                       프로필                        |

---


