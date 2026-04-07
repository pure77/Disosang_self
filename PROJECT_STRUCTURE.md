# 프로젝트 구조 정리

## 1. 문서 목적
- 이 문서는 현재 저장소의 디렉터리 구조와 주요 역할을 빠르게 파악하기 위한 내부용 정리 문서다.
- 작업 규칙은 `AGENTS.md`를 보고, 프로젝트 구조는 이 문서를 보면 된다.

## 2. 저장소 전체 구조
```text
Disosang_self/
├─ AGENTS.md
├─ PROJECT_STRUCTURE.md
├─ load_test/
└─ disosang/
```

### 루트 설명
- `AGENTS.md`
  - 작업 규칙, 응답 방식, 수정 전 확인 원칙이 들어 있는 문서
- `PROJECT_STRUCTURE.md`
  - 현재 프로젝트 구조를 빠르게 보기 위한 문서
- `load_test`
  - k6 성능 테스트 스크립트와 HTML 결과 리포트 보관
- `disosang`
  - 실제 Spring Boot 애플리케이션 코드

## 3. load_test 구조
- `store-test.js`
  - `/store/map/search` API 대상 k6 부하 테스트 스크립트
- `summary*.html`
  - 테스트 실행 결과로 생성된 리포트 파일

### load_test 해석
- 이 폴더는 서비스 기능 코드가 아니라 성능 측정 산출물 영역이다.
- 검색 API 성능을 신경 쓰고 있다는 흔적이므로, 추후 검색 쿼리 수정 시 같이 참고할 가치가 있다.

## 4. disosang 애플리케이션 구조

### 4.1 빌드 및 실행 관련
- `build.gradle`
  - Gradle 의존성 및 플러그인 설정
  - 주요 기술 스택:
    - Spring Boot 3.5.5
    - Spring Web
    - Spring Data JPA
    - Spring Security
    - Thymeleaf
    - MySQL Driver
    - H2
    - springdoc-openapi
    - OpenCSV
- `settings.gradle`
  - Gradle 프로젝트 이름/설정
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
  - Gradle Wrapper

### 4.2 소스 루트
```text
disosang/src/
├─ main/
│  ├─ java/com/pmh/disosang/
│  └─ resources/
└─ test/
```

## 5. Java 패키지 구조

### 5.1 최상위 패키지
```text
com.pmh.disosang
├─ config
├─ map
│  ├─ home
│  └─ store
├─ review
└─ user
```

### 5.2 공통 진입점
- `DisosangApplication.java`
  - Spring Boot 메인 실행 클래스
- `IndexController.java`
  - 루트 경로 `/` 요청을 받아 `welcome.html` 반환

## 6. 패키지별 역할

### 6.1 config
- `SecurityConfig.java`
  - Spring Security 인증/인가 설정
  - 비로그인 허용 경로 설정
  - 로그인 페이지, 로그인 처리 URL, 로그인 성공/실패 이동 경로 설정
  - 비밀번호 인코더 등록
- `WebConfig.java`
  - 웹 MVC 관련 설정 담당

### 6.2 user
```text
user/
├─ controller
├─ dto/request
├─ entity
├─ service
└─ UserRepository.java
```

- 역할
  - 회원가입
  - 로그인 연동
  - 로그인한 사용자 정보 조회
- 주요 파일
  - `UserController.java`
    - 회원가입 처리
    - 내 정보 페이지 데이터 바인딩
  - `UserPageController.java`
    - 회원가입/로그인 화면 반환
  - `UserService.java`
    - 회원가입 로직
    - 이메일 기반 사용자 조회
    - `UserDetailsService` 구현으로 Spring Security 로그인 연동
  - `User.java`
    - 사용자 엔티티
    - `UserDetails` 구현체
  - `UserRepository.java`
    - 사용자 DB 접근

### 6.3 map.home
- `HomeController.java`
  - `/home/home` 요청 처리
  - 홈 화면 반환

### 6.4 map.store
```text
map/store/
├─ controller
├─ dto/request
├─ dto/response
├─ entity
├─ service
├─ CategoryRepository.java
└─ StoreRepository.java
```

- 역할
  - 지도 화면 제공
  - 가게 검색
  - 가게 상세 조회
  - 카테고리 기반 검색 보조
- 주요 파일
  - `StoreController.java`
    - 지도 페이지 반환
    - `/store/map/search` 검색 API
    - `/store/detail/{storeId}` 상세 페이지
  - `StoreService.java`
    - 검색 핵심 로직 담당
    - 키워드 정규화
    - 정확 검색
    - 카테고리 확장 검색
    - 짧은 키워드 검색
    - 오타 보정용 fuzzy 검색
  - `StoreRepository.java`
    - 네이티브 SQL 기반 검색 쿼리 보유
    - 위치 범위 + 거리 정렬 + 점수 기반 검색 로직 포함
  - `CategoryRepository.java`
    - 카테고리명 검색
    - 재귀 CTE로 하위 카테고리 조회
  - `Store.java`
    - 가게 엔티티
    - 평균 평점 / 리뷰 수 갱신 메서드 포함
  - `Category.java`
    - 카테고리 엔티티
  - `StoreBatchService.java`, `StoreBatchController.java`
    - 가게 데이터 적재 관련 코드
    - 현재 컨트롤러는 대부분 주석 처리 상태

### 6.5 review
```text
review/
├─ controller
├─ dto/request
├─ dto/response
├─ entity
├─ service
└─ ReviewRepository.java
```

- 역할
  - 리뷰 작성/수정/삭제
  - 리뷰 목록 조회
  - 사진 업로드/삭제
- 주요 파일
  - `ReviewController.java`
    - 리뷰 등록, 수정, 삭제 요청 처리
  - `ReviewService.java`
    - 리뷰 생성/수정/삭제 핵심 로직
    - 사진 엔티티 연결
    - 가게 평점/리뷰 수 갱신
  - `FileService.java`
    - 업로드 파일 저장
    - 저장 URL 생성
    - 파일 삭제 처리
  - `ReviewRepository.java`
    - 특정 가게의 리뷰 조회
    - fetch join 기반 조회
  - `Review.java`
    - 리뷰 엔티티
  - `Photo.java`
    - 리뷰 사진 엔티티

## 7. resources 구조

### 7.1 templates
```text
templates/
├─ welcome.html
├─ home/
│  └─ home.html
├─ store/
│  ├─ map.html
│  └─ detail.html
└─ user/
   ├─ login.html
   ├─ signup.html
   └─ userInfo.html
```

- 특징
  - 프론트엔드가 별도 SPA로 분리된 구조가 아니다.
  - 서버에서 Thymeleaf 템플릿을 렌더링하는 방식이다.

### 7.2 static
```text
static/
├─ css/
│  ├─ detail.css
│  ├─ map.css
│  ├─ signup.css
│  ├─ userInfo.css
│  └─ welcome.css
└─ js/
   ├─ detail.js
   ├─ map.js
   └─ signup.js
```

- 화면별로 CSS/JS가 분리되어 있다.
- 지도, 상세 페이지, 회원가입 페이지에 개별 스크립트가 연결되는 구조다.

## 8. 설정 파일 구조

### `src/main/resources/application.yml`
- `dev`, `test`, `prod` 프로필 분리
- `test`
  - H2 메모리 DB 사용
- `dev`
  - MySQL 사용
  - `application-secret.yml` optional import
- `prod`
  - 환경변수 기반 MySQL 및 JWT 설정 사용

### 기타 설정 파일
- `application.properties`
  - 추가 Spring 설정
- `logback-spring.xml`
  - 로그 설정

## 9. 테스트 구조
- `src/test/java/com/pmh/disosang/DisosangApplicationTests.java`
  - 현재는 기본 애플리케이션 로딩 테스트 수준

### 현재 테스트 상태 해석
- 테스트 코드가 많지 않다.
- 핵심 비즈니스 로직인 검색, 리뷰, 사용자 흐름에 대한 단위/통합 테스트는 거의 없는 상태로 보인다.

## 10. 이 프로젝트를 볼 때 핵심 포인트
- 백엔드 기준 핵심 도메인은 `store 검색`, `review`, `user` 세 축이다.
- 화면은 Thymeleaf 기반 서버 렌더링이다.
- 검색 로직은 일반 CRUD보다 복잡하며, 네이티브 SQL과 위치 기반 조건이 핵심이다.
- 리뷰는 사진 업로드와 평점 집계를 같이 다룬다.
- 성능 테스트 파일이 별도로 있는 것으로 봐서 검색 API가 주요 관심 구간이다.

## 11. 빠르게 파일 찾는 기준
- 로그인/회원가입 수정
  - `user/controller`, `user/service`, `templates/user`
- 지도 검색 수정
  - `map/store/controller/StoreController.java`
  - `map/store/service/StoreService.java`
  - `map/store/StoreRepository.java`
  - `static/js/map.js`
  - `templates/store/map.html`
- 가게 상세 수정
  - `templates/store/detail.html`
  - `static/js/detail.js`
  - `map/store/controller/StoreController.java`
- 리뷰 기능 수정
  - `review/controller`
  - `review/service`
  - `review/entity`
- 업로드 경로/파일 처리 수정
  - `review/service/FileService.java`
- 보안 설정 수정
  - `config/SecurityConfig.java`

## 12. 참고 메모
- `.gradle`, `build`, `out`, `.idea`는 개발 산출물/도구 설정 영역이라 구조 파악 우선순위는 낮다.
- 실제 기능 파악은 `src/main/java`, `src/main/resources`, `load_test/store-test.js`를 먼저 보면 된다.
