# 즐겨찾기 + 지도 핀 라벨 + 상세페이지 크기 통일 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 천안사랑카드(디소상) 앱에 즐겨찾기 기능을 추가하고, 지도 검색 핀에 가게이름·별점 라벨(즐겨찾기는 노란 별)을 표시하며, 상세페이지 폭을 다른 화면과 통일한다.

**Architecture:** 새 `Favorite` 도메인(JPA 엔티티 + 리포지토리 + 서비스 + REST 컨트롤러)을 추가한다. 즐겨찾기 토글은 `/favorites/toggle` JSON API로 처리하고, 지도 검색 응답(`StoreResponse`)에 `favorite` 플래그를 더해 프론트(`map.js`)가 핀 모양을 결정한다. 즐겨찾기 여부 마킹은 `StoreService`를 건드리지 않고 `StoreController`에서 수행해 기존 검색 단위 테스트를 그대로 유지한다.

**Tech Stack:** Spring Boot 3 (Spring MVC, Spring Security, Spring Data JPA), Thymeleaf, MySQL(운영/dev)·H2(test), Kakao Maps JS SDK, 바닐라 JS, Gradle, JUnit5 + Mockito + AssertJ.

---

## File Structure

신규 파일:
- `disosang/src/main/java/com/pmh/disosang/favorite/entity/Favorite.java` — 즐겨찾기 엔티티
- `disosang/src/main/java/com/pmh/disosang/favorite/FavoriteRepository.java` — 리포지토리
- `disosang/src/main/java/com/pmh/disosang/favorite/dto/request/FavoriteToggleRequest.java` — 토글 요청 바디
- `disosang/src/main/java/com/pmh/disosang/favorite/service/FavoriteService.java` — 토글/조회 로직
- `disosang/src/main/java/com/pmh/disosang/favorite/controller/FavoriteController.java` — `/favorites/toggle` API
- `disosang/src/test/java/com/pmh/disosang/favorite/service/FavoriteServiceTest.java` — 서비스 단위 테스트
- `disosang/sql/2026-06-12_create_favorite.sql` — 운영용 DDL

수정 파일:
- `disosang/src/main/java/com/pmh/disosang/map/store/dto/response/StoreResponse.java` — `favorite` 필드 추가
- `disosang/src/main/java/com/pmh/disosang/map/store/controller/StoreController.java` — 검색/상세에 user·favorite 연동
- `disosang/src/main/java/com/pmh/disosang/map/home/HomeController.java` — 홈에 즐겨찾기 목록 주입
- `disosang/src/main/resources/templates/store/detail.html` — 즐겨찾기 버튼 + CSRF meta
- `disosang/src/main/resources/templates/home/home.html` — 결제 UI 제거 → 즐겨찾기 목록
- `disosang/src/main/resources/static/js/detail.js` — 즐겨찾기 토글 fetch
- `disosang/src/main/resources/static/js/map.js` — CustomOverlay 라벨/별 핀
- `disosang/src/main/resources/static/css/detail.css` — 420px 폰 프레임 통일
- `disosang/src/main/resources/static/css/map.css` — 오버레이 라벨/핀 스타일

> 참고: `config/SecurityConfig.java`는 변경하지 않는다. `/favorites/**`는 기본 `authenticated()`로 보호되고, `/store/map/search`는 permitAll 유지(익명이면 favorite=false 처리).

---

## Task 1: Favorite 엔티티 + 리포지토리

**Files:**
- Create: `disosang/src/main/java/com/pmh/disosang/favorite/entity/Favorite.java`
- Create: `disosang/src/main/java/com/pmh/disosang/favorite/FavoriteRepository.java`

- [ ] **Step 1: Favorite 엔티티 작성**

`Review` 엔티티(`review/entity/Review.java`)의 `@ManyToOne(LAZY)` + `@PrePersist` 패턴을 따른다.

```java
package com.pmh.disosang.favorite.entity;

import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 즐겨찾기
 * - 한 사용자가 한 가게를 즐겨찾기한 관계.
 * - (user_id, store_id) 조합은 유일해야 하므로 유니크 제약을 둡니다.
 */
@Entity
@Table(
        name = "favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_user_store",
                columnNames = {"user_id", "store_id"}
        )
)
@Getter
@NoArgsConstructor
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Favorite(User user, Store store) {
        this.user = user;
        this.store = store;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: FavoriteRepository 작성**

```java
package com.pmh.disosang.favorite;

import com.pmh.disosang.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUser_IdAndStore_StoreId(Long userId, Long storeId);

    Optional<Favorite> findByUser_IdAndStore_StoreId(Long userId, Long storeId);

    @Query("select f.store.storeId from Favorite f where f.user.id = :userId")
    List<Long> findStoreIdsByUserId(@Param("userId") Long userId);

    /*
     * 홈 즐겨찾기 목록용 — store를 fetch join 으로 함께 조회해 N+1을 방지합니다.
     */
    @Query("select f from Favorite f join fetch f.store where f.user.id = :userId order by f.createdAt desc")
    List<Favorite> findByUserIdWithStore(@Param("userId") Long userId);
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `cd disosang; .\gradlew.bat compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add disosang/src/main/java/com/pmh/disosang/favorite/entity/Favorite.java disosang/src/main/java/com/pmh/disosang/favorite/FavoriteRepository.java
git commit -m "feat(favorite): add Favorite entity and repository"
```

---

## Task 2: FavoriteService (TDD)

**Files:**
- Create: `disosang/src/main/java/com/pmh/disosang/favorite/service/FavoriteService.java`
- Test: `disosang/src/test/java/com/pmh/disosang/favorite/service/FavoriteServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

기존 `StoreServiceFuzzySearchTest`의 Mockito 스타일을 따른다.

```java
package com.pmh.disosang.favorite.service;

import com.pmh.disosang.favorite.FavoriteRepository;
import com.pmh.disosang.favorite.entity.Favorite;
import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User userWithId(Long id) {
        return User.builder().id(id).name("u").email("u@e.com").password("p").build();
    }

    @Test
    void 즐겨찾기가_없으면_추가하고_true를_반환한다() {
        User user = userWithId(1L);
        Store store = Store.builder().storeId(10L).placeName("가게").storeType("일반음식점").build();
        given(favoriteRepository.findByUser_IdAndStore_StoreId(1L, 10L)).willReturn(Optional.empty());
        given(storeRepository.findById(10L)).willReturn(Optional.of(store));

        boolean result = favoriteService.toggle(user, 10L);

        assertThat(result).isTrue();
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void 이미_즐겨찾기면_삭제하고_false를_반환한다() {
        User user = userWithId(1L);
        Favorite existing = mock(Favorite.class);
        given(favoriteRepository.findByUser_IdAndStore_StoreId(1L, 10L)).willReturn(Optional.of(existing));

        boolean result = favoriteService.toggle(user, 10L);

        assertThat(result).isFalse();
        verify(favoriteRepository).delete(existing);
        verify(storeRepository, never()).findById(any());
    }
}
```

- [ ] **Step 2: 테스트가 실패(컴파일 에러)하는지 확인**

Run: `cd disosang; .\gradlew.bat test --tests "com.pmh.disosang.favorite.service.FavoriteServiceTest"`
Expected: FAIL — `FavoriteService` 클래스 없음(컴파일 에러)

- [ ] **Step 3: FavoriteService 구현**

```java
package com.pmh.disosang.favorite.service;

import com.pmh.disosang.favorite.FavoriteRepository;
import com.pmh.disosang.favorite.entity.Favorite;
import com.pmh.disosang.map.store.StoreRepository;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.entity.Store;
import com.pmh.disosang.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StoreRepository storeRepository;

    /**
     * 즐겨찾기 토글
     * - 이미 즐겨찾기한 가게면 삭제 후 false 반환
     * - 아니면 새로 추가 후 true 반환
     */
    public boolean toggle(User user, Long storeId) {
        return favoriteRepository.findByUser_IdAndStore_StoreId(user.getId(), storeId)
                .map(existing -> {
                    favoriteRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    Store store = storeRepository.findById(storeId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 가게입니다."));
                    favoriteRepository.save(Favorite.builder().user(user).store(store).build());
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long storeId) {
        return favoriteRepository.existsByUser_IdAndStore_StoreId(userId, storeId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getFavoriteStoreIds(Long userId) {
        return new HashSet<>(favoriteRepository.findStoreIdsByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getFavoriteStores(Long userId) {
        return favoriteRepository.findByUserIdWithStore(userId).stream()
                .map(favorite -> StoreResponse.fromEntity(favorite.getStore()))
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd disosang; .\gradlew.bat test --tests "com.pmh.disosang.favorite.service.FavoriteServiceTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add disosang/src/main/java/com/pmh/disosang/favorite/service/FavoriteService.java disosang/src/test/java/com/pmh/disosang/favorite/service/FavoriteServiceTest.java
git commit -m "feat(favorite): add FavoriteService toggle with tests"
```

---

## Task 3: 즐겨찾기 토글 API (Controller) + 마이그레이션 SQL

**Files:**
- Create: `disosang/src/main/java/com/pmh/disosang/favorite/dto/request/FavoriteToggleRequest.java`
- Create: `disosang/src/main/java/com/pmh/disosang/favorite/controller/FavoriteController.java`
- Create: `disosang/sql/2026-06-12_create_favorite.sql`

- [ ] **Step 1: 요청 DTO 작성**

```java
package com.pmh.disosang.favorite.dto.request;

/**
 * 즐겨찾기 토글 요청 바디
 */
public record FavoriteToggleRequest(Long storeId) {
}
```

- [ ] **Step 2: 컨트롤러 작성**

`ReviewController`의 `@AuthenticationPrincipal User` 패턴을 따른다. user==null이면 401, storeId 누락이면 400.

```java
package com.pmh.disosang.favorite.controller;

import com.pmh.disosang.favorite.dto.request.FavoriteToggleRequest;
import com.pmh.disosang.favorite.service.FavoriteService;
import com.pmh.disosang.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggle(@RequestBody FavoriteToggleRequest request,
                                    @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }
        if (request == null || request.storeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "가게 정보가 필요합니다."));
        }

        boolean favorite = favoriteService.toggle(user, request.storeId());
        return ResponseEntity.ok(Map.of("favorite", favorite));
    }
}
```

- [ ] **Step 3: 마이그레이션 SQL 작성**

기존 `disosang/sql/2026-04-13_create_store_search_token.sql` 형식(주석 + 유니크키 + FK)을 따른다. dev는 `ddl-auto: update`로 자동 생성되지만 운영(`ddl-auto: none`)을 위해 작성한다.

```sql
/*
 * favorite
 * - 사용자가 가게를 즐겨찾기한 관계를 저장하는 테이블입니다.
 *
 * 왜 필요한가?
 * - 상세페이지에서 즐겨찾기 토글, 홈 화면의 즐겨찾기 목록, 지도의 노란 별 핀 표시에 사용됩니다.
 *
 * 정책:
 * - (user_id, store_id) 조합은 한 번만 존재해야 하므로 유니크 제약을 둡니다.
 * - 회원/가게가 삭제되면 즐겨찾기도 함께 정리되도록 ON DELETE CASCADE 를 둡니다.
 */
CREATE TABLE favorite (
    favorite_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    store_id    BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (favorite_id),

    /*
     * 같은 사용자가 같은 가게를 중복 즐겨찾기하지 못하도록 막습니다.
     */
    CONSTRAINT uk_favorite_user_store UNIQUE (user_id, store_id),

    /*
     * 특정 사용자의 즐겨찾기 목록 조회용 인덱스
     */
    KEY idx_favorite_user (user_id),

    CONSTRAINT fk_favorite_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_favorite_store
        FOREIGN KEY (store_id) REFERENCES store(store_id)
        ON DELETE CASCADE
);
```

- [ ] **Step 4: 컴파일 확인**

Run: `cd disosang; .\gradlew.bat compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add disosang/src/main/java/com/pmh/disosang/favorite/dto/ disosang/src/main/java/com/pmh/disosang/favorite/controller/ disosang/sql/2026-06-12_create_favorite.sql
git commit -m "feat(favorite): add toggle API and migration SQL"
```

---

## Task 4: StoreResponse에 favorite 플래그 추가

**Files:**
- Modify: `disosang/src/main/java/com/pmh/disosang/map/store/dto/response/StoreResponse.java`

- [ ] **Step 1: favorite 필드 + 세터 추가**

클래스 상단 어노테이션에 `@Setter`를 추가하고 `favorite` 필드를 추가한다. 변경 후 파일 전체는 아래와 같다.

```java
package com.pmh.disosang.map.store.dto.response;

import com.pmh.disosang.map.store.entity.Store;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 가맹점 응답용 DTO (클라이언트 응답 전용)
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class StoreResponse {

    private Long id;
    private String placeName;

    // 화면에 보여줄 카테고리명
    private String category;

    private String addressName;
    private String roadAddressName;
    private String phone;
    private String storeType;

    private Double x;  // 경도
    private Double y;  // 위도

    private Double averageRating;
    private Integer reviewCount;
    private String thumbnailUrl;

    // 현재 로그인 사용자가 즐겨찾기한 가게인지 여부 (지도 검색에서만 채워짐)
    private boolean favorite;

    public static StoreResponse fromEntity(Store store) {
        return StoreResponse.builder()
                .id(store.getStoreId())
                .placeName(store.getPlaceName())
                .category(
                        store.getCategory() != null
                                ? store.getCategory().getName()
                                : null
                )
                .addressName(store.getAddressName())
                .roadAddressName(store.getRoadAddressName())
                .phone(store.getPhone())
                .storeType(store.getStoreType())
                .x(store.getX())
                .y(store.getY())
                .averageRating(store.getAverageRating())
                .reviewCount(store.getReviewCount())
                .thumbnailUrl(store.getThumbnailUrl())
                .favorite(false)
                .build();
    }
}
```

- [ ] **Step 2: 기존 테스트가 깨지지 않는지 확인**

Run: `cd disosang; .\gradlew.bat test --tests "com.pmh.disosang.map.store.service.StoreServiceFuzzySearchTest"`
Expected: PASS (StoreService 시그니처 변경 없음 → 기존 테스트 영향 없음)

- [ ] **Step 3: 커밋**

```bash
git add disosang/src/main/java/com/pmh/disosang/map/store/dto/response/StoreResponse.java
git commit -m "feat(store): add favorite flag to StoreResponse"
```

---

## Task 5: 지도 검색 + 상세페이지 컨트롤러에 즐겨찾기 연동

**Files:**
- Modify: `disosang/src/main/java/com/pmh/disosang/map/store/controller/StoreController.java`

> 설계 노트: `StoreService.searchStoresInMap`은 **변경하지 않는다.** 즐겨찾기 마킹은 컨트롤러에서 결과 DTO에 적용한다. 이렇게 하면 `StoreService`의 기존 단위 테스트가 그대로 유지되고 책임 분리가 깔끔하다.

- [ ] **Step 1: StoreController 수정**

`FavoriteService` 주입, 검색에 `@AuthenticationPrincipal User user`(익명이면 null) 추가 후 favorite 마킹, 상세에 `isFavorite` 모델 추가. 변경 후 파일 전체는 아래와 같다.

```java
package com.pmh.disosang.map.store.controller;

import com.pmh.disosang.favorite.service.FavoriteService;
import com.pmh.disosang.map.store.dto.request.StoreSearchRequest;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.service.StoreService;
import com.pmh.disosang.review.dto.response.ReviewResponse;
import com.pmh.disosang.review.service.ReviewService;
import com.pmh.disosang.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;

    @Value("${kakao.maps.js.key}")
    private String kakaoJsKey;

    @GetMapping("/map")
    public String mapPage(Model model) {
        model.addAttribute("kakaoKey", kakaoJsKey);
        return "store/map";
    }

    @GetMapping("/map/search")
    public ResponseEntity<?> searchStoresInMap(@Valid @ModelAttribute StoreSearchRequest request,
                                               BindingResult bindingResult,
                                               @AuthenticationPrincipal User user) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", bindingResult.getAllErrors().get(0).getDefaultMessage()));
        }

        List<StoreResponse> storeResponses = storeService.searchStoresInMap(
                request.getKeyword().trim(),
                request.getCenterY(),
                request.getCenterX(),
                request.getMinY(),
                request.getMaxY(),
                request.getMinX(),
                request.getMaxX()
        );

        // 로그인 사용자라면 즐겨찾기한 가게를 표시 (익명이면 모두 false 유지)
        if (user != null) {
            Set<Long> favoriteIds = favoriteService.getFavoriteStoreIds(user.getId());
            storeResponses.forEach(store -> store.setFavorite(favoriteIds.contains(store.getId())));
        }

        return ResponseEntity.ok(storeResponses);
    }

    @GetMapping("/detail/{storeId}")
    public String storeDetail(@PathVariable("storeId") Long storeId,
                              @RequestParam(name = "sort", defaultValue = "newest") String sort,
                              @AuthenticationPrincipal User user,
                              Model model) {
        StoreResponse storeInfo = storeService.findById(storeId);
        List<ReviewResponse> reviews = reviewService.getReviews(storeId, sort);

        boolean isFavorite = user != null && favoriteService.isFavorite(user.getId(), storeId);

        model.addAttribute("store", storeInfo);
        model.addAttribute("reviews", reviews);
        model.addAttribute("sort", sort);
        model.addAttribute("isFavorite", isFavorite);
        return "store/detail";
    }
}
```

- [ ] **Step 2: 컴파일 + 기존 테스트 확인**

Run: `cd disosang; .\gradlew.bat compileJava test`
Expected: BUILD SUCCESSFUL, 모든 테스트 PASS

- [ ] **Step 3: 커밋**

```bash
git add disosang/src/main/java/com/pmh/disosang/map/store/controller/StoreController.java
git commit -m "feat(store): mark favorites on map search and detail page"
```

---

## Task 6: 상세페이지 즐겨찾기 버튼 + CSRF + 크기 통일

**Files:**
- Modify: `disosang/src/main/resources/templates/store/detail.html`
- Modify: `disosang/src/main/resources/static/js/detail.js`
- Modify: `disosang/src/main/resources/static/css/detail.css`

- [ ] **Step 1: detail.html `<head>`에 CSRF meta 추가**

`detail.html`의 `<link rel="stylesheet" href="/css/detail.css">` 바로 다음 줄에 추가:

```html
    <meta name="_csrf" th:content="${_csrf.token}">
    <meta name="_csrf_header" th:content="${_csrf.headerName}">
```

- [ ] **Step 2: detail.html 즐겨찾기 버튼 교체**

기존 줄:

```html
        <a href="#" class="btn"><span>⭐</span> 즐겨찾기</a>
```

를 다음으로 교체(아이콘은 `isFavorite`에 따라 채워진 별/빈 별):

```html
        <button type="button" id="favoriteBtn" class="btn favorite-btn"
                th:classappend="${isFavorite} ? ' active' : ''"
                th:data-store-id="${store.id}">
            <span class="fav-icon" th:text="${isFavorite} ? '★' : '☆'">☆</span> 즐겨찾기
        </button>
```

- [ ] **Step 3: detail.css에 버튼/420px 스타일 적용**

(a) `.detail-container`의 `max-width: 600px;`를 `max-width: 420px;`로 바꾸고, 홈/내정보와 동일한 폰 프레임 느낌을 위해 블록을 다음으로 교체:

```css
.detail-container {
    max-width: 420px;
    margin: 0 auto;
    min-height: 100vh;
    background-color: #fff;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
```

또한 `body`의 배경을 홈과 맞추기 위해 `background-color: #f5f5f5;`를 `background-color: #f0f1f5;`로 바꾼다.

(b) `.bottom-nav`의 `max-width: 600px;`를 `max-width: 420px;`로 바꾼다.

(c) 파일 끝에 즐겨찾기 버튼 스타일 추가:

```css
/* 즐겨찾기 버튼 (action-buttons 안의 button 을 a.btn 과 동일하게 보이도록) */
.action-buttons .favorite-btn {
    background: none;
    border: none;
    cursor: pointer;
    font-family: 'Noto Sans KR', sans-serif;
    padding: 0;
}
.action-buttons .favorite-btn.active {
    color: #f0ad4e;
    font-weight: 700;
}
.action-buttons .favorite-btn.active .fav-icon {
    color: #f0ad4e;
}
```

- [ ] **Step 4: detail.js에 토글 핸들러 추가**

`detail.js` 파일 맨 끝(마지막 `});` 밖, 파일 최하단)에 추가:

```javascript
// =========================================
// 즐겨찾기 토글
// =========================================
document.addEventListener('DOMContentLoaded', function () {
    const favoriteBtn = document.getElementById('favoriteBtn');
    if (!favoriteBtn) return;

    favoriteBtn.addEventListener('click', async function () {
        const storeId = favoriteBtn.getAttribute('data-store-id');
        const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

        try {
            const res = await fetch('/favorites/toggle', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify({ storeId: Number(storeId) })
            });

            if (res.status === 401) {
                window.location.href = '/user/login';
                return;
            }
            if (!res.ok) {
                alert('즐겨찾기 처리에 실패했습니다.');
                return;
            }

            const data = await res.json();
            const icon = favoriteBtn.querySelector('.fav-icon');
            if (data.favorite) {
                favoriteBtn.classList.add('active');
                if (icon) icon.textContent = '★';
            } else {
                favoriteBtn.classList.remove('active');
                if (icon) icon.textContent = '☆';
            }
        } catch (e) {
            alert('즐겨찾기 처리 중 오류가 발생했습니다.');
        }
    });
});
```

- [ ] **Step 5: 빌드 확인 후 커밋**

Run: `cd disosang; .\gradlew.bat compileJava`
Expected: BUILD SUCCESSFUL

```bash
git add disosang/src/main/resources/templates/store/detail.html disosang/src/main/resources/static/js/detail.js disosang/src/main/resources/static/css/detail.css
git commit -m "feat(detail): favorite toggle button and unify page width to 420px"
```

---

## Task 7: 지도 핀 라벨 + 즐겨찾기 노란 별 핀

**Files:**
- Modify: `disosang/src/main/resources/static/js/map.js`
- Modify: `disosang/src/main/resources/static/css/map.css`

- [ ] **Step 1: map.js — 별 SVG 빌더 추가**

`buildMarkerSvg(fillColor)` 함수 바로 아래에 노란 별 핀 SVG 빌더를 추가한다.

```javascript
function buildStarMarkerSvg() {
    return `
        <svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" viewBox="0 0 24 24">
            <path d="M12 2l2.9 6.26 6.9.6-5.2 4.52 1.56 6.74L12 17.27 5.84 20.12 7.4 13.38 2.2 8.86l6.9-.6L12 2z"
                  fill="#f5c518" stroke="#e0a800" stroke-width="1"/>
        </svg>
    `.trim();
}
```

- [ ] **Step 2: map.js — `showStores()`를 CustomOverlay 방식으로 교체**

`markers` 배열은 위치 마커뿐 아니라 라벨까지 담은 CustomOverlay를 보관하도록 그대로 재사용한다(`setMap(null)` 호환). `showStores()` 함수 전체를 아래로 교체한다.

```javascript
function showStores() {
    markers.forEach((marker) => marker.setMap(null));
    markers = [];

    const listDiv = document.getElementById('storeList');
    listDiv.innerHTML = '';

    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    const stores = currentStores.slice(start, end);

    stores.forEach((store) => {
        const position = new kakao.maps.LatLng(store.y, store.x);

        const overlay = new kakao.maps.CustomOverlay({
            map: map,
            position: position,
            content: createPinElement(store, position),
            yAnchor: 1,
            clickable: true,
            zIndex: store.favorite ? 5 : 3
        });
        markers.push(overlay);

        const item = document.createElement('div');
        item.className = 'store-item';
        item.innerHTML = `<strong>${store.placeName}</strong><br><small>${store.addressName}</small>`;
        item.addEventListener('click', () => {
            map.setCenter(position);
            showStoreInfoInSheet(store);
        });
        listDiv.appendChild(item);
    });

    renderPagination();
}

function createPinElement(store, position) {
    const wrap = document.createElement('div');
    wrap.className = 'map-pin' + (store.favorite ? ' favorite' : '');

    // 라벨: 별점(있으면) + 가게이름
    const label = document.createElement('div');
    label.className = 'map-pin-label';
    const hasRating = store.averageRating && store.averageRating > 0;
    label.innerHTML = (hasRating
        ? `<span class="map-pin-star">★ ${store.averageRating}</span> `
        : '') + `<span class="map-pin-name">${store.placeName}</span>`;

    // 핀 아이콘: 즐겨찾기면 노란 별, 아니면 업종별 색상 핀
    const icon = document.createElement('div');
    icon.className = 'map-pin-icon';
    icon.innerHTML = store.favorite ? buildStarMarkerSvg() : buildMarkerSvg(pinColor(store));

    wrap.appendChild(label);
    wrap.appendChild(icon);

    wrap.addEventListener('click', function () {
        map.setCenter(position);
        showStoreInfoInSheet(store);
    });

    return wrap;
}

function pinColor(store) {
    const storeType = (store.storeType || '').toLowerCase();
    if (storeType === 'cheap') return '#27ae60';
    if (storeType === 'tm') return '#f39c12';
    return '#0075ff';
}
```

> 참고: 기존 `getMarkerImage`/`getMarkerOptions` 함수는 더 이상 사용되지 않지만 삭제하지 않아도 동작에는 영향이 없다. 깔끔하게 정리하려면 두 함수와 `markerImageCache` 선언을 제거해도 된다(제거 시 다른 참조가 없는지 확인).

- [ ] **Step 3: map.css — 핀/라벨 스타일 추가**

`map.css` 파일 끝에 추가한다.

```css
/* 지도 커스텀 핀 (이름+별점 라벨) */
.map-pin {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    transform: translateY(-2px);
}
.map-pin-label {
    background: #fff;
    border-radius: 14px;
    padding: 3px 8px;
    font-size: 12px;
    font-weight: 500;
    color: #333;
    white-space: nowrap;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
    margin-bottom: 2px;
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
}
.map-pin-star {
    color: #f0ad4e;
    font-weight: 700;
}
.map-pin.favorite .map-pin-label {
    border: 1px solid #f5c518;
}
.map-pin-icon {
    line-height: 0;
}
```

- [ ] **Step 4: 빌드 확인 후 커밋**

Run: `cd disosang; .\gradlew.bat compileJava`
Expected: BUILD SUCCESSFUL (JS/CSS는 정적 리소스라 컴파일 영향 없음 — 빌드 성공 확인용)

```bash
git add disosang/src/main/resources/static/js/map.js disosang/src/main/resources/static/css/map.css
git commit -m "feat(map): show name+rating pin labels and yellow star for favorites"
```

---

## Task 8: 홈 화면 → 즐겨찾기 목록

**Files:**
- Modify: `disosang/src/main/java/com/pmh/disosang/map/home/HomeController.java`
- Modify: `disosang/src/main/resources/templates/home/home.html`

- [ ] **Step 1: HomeController 수정**

```java
package com.pmh.disosang.map.home;

import com.pmh.disosang.favorite.service.FavoriteService;
import com.pmh.disosang.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final FavoriteService favoriteService;

    @GetMapping("/home/home")
    public String home(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("favorites",
                user != null ? favoriteService.getFavoriteStores(user.getId()) : List.of());
        return "home/home"; // templates/home/home.html 호출
    }
}
```

- [ ] **Step 2: home.html 본문 교체 — 결제 UI 제거, 즐겨찾기 목록 표시**

`<div class="main-content">` 안의 기존 내용(`<header>` 다음의 `main-card` div와 `history-card` div 전체)을 아래로 교체한다. `<header><h1>홈</h1></header>`는 유지하고, 그 아래의 두 카드(`main-card`, `history-card`)를 다음으로 대체:

```html
        <header>
            <h1>즐겨찾기</h1>
        </header>

        <div th:if="${#lists.isEmpty(favorites)}" class="empty-fav">
            <div class="empty-icon">⭐</div>
            <p>즐겨찾기한 가게가 없습니다.</p>
            <a th:href="@{/store/map}" class="empty-link">지도에서 가게 추가하기</a>
        </div>

        <a th:each="store : ${favorites}"
           th:href="@{/store/detail/{id}(id=${store.id})}"
           class="card fav-card">
            <div class="fav-info">
                <div class="fav-name" th:text="${store.placeName}">가게 이름</div>
                <div class="fav-rating">
                    <span class="fav-star">★</span>
                    <span th:text="${store.averageRating != null and store.averageRating > 0} ? ${store.averageRating} : '별점 없음'">0.0</span>
                </div>
                <div class="fav-address"
                     th:text="${store.roadAddressName != null and !store.roadAddressName.isEmpty()} ? ${store.roadAddressName} : ${store.addressName}">주소</div>
            </div>
        </a>
```

- [ ] **Step 3: home.html `<style>`에 즐겨찾기 카드 스타일 추가**

`</style>` 바로 위에 추가:

```css
        /* 즐겨찾기 카드 */
        .fav-card {
            display: block;
            text-decoration: none;
            color: inherit;
        }
        .fav-card .fav-name {
            font-size: 18px;
            font-weight: 700;
            margin-bottom: 6px;
        }
        .fav-card .fav-rating {
            font-size: 14px;
            color: #555;
            margin-bottom: 6px;
        }
        .fav-card .fav-star {
            color: #f0ad4e;
        }
        .fav-card .fav-address {
            font-size: 13px;
            color: #888;
        }
        .empty-fav {
            text-align: center;
            color: #888;
            margin-top: 80px;
        }
        .empty-fav .empty-icon {
            font-size: 48px;
            margin-bottom: 12px;
        }
        .empty-fav .empty-link {
            display: inline-block;
            margin-top: 12px;
            color: #0075ff;
            text-decoration: none;
            font-weight: 500;
        }
```

- [ ] **Step 4: 빌드 확인 후 커밋**

Run: `cd disosang; .\gradlew.bat compileJava`
Expected: BUILD SUCCESSFUL

```bash
git add disosang/src/main/java/com/pmh/disosang/map/home/HomeController.java disosang/src/main/resources/templates/home/home.html
git commit -m "feat(home): replace payment UI with favorites list"
```

---

## Task 9: 전체 빌드 + 수동 검증

**Files:** 없음 (검증 단계)

- [ ] **Step 1: 전체 테스트 실행**

Run: `cd disosang; .\gradlew.bat test`
Expected: BUILD SUCCESSFUL, 모든 테스트 PASS (FavoriteServiceTest 2개 + 기존 StoreServiceFuzzySearchTest 4개)

- [ ] **Step 2: 앱 실행 (dev 프로필, MySQL)**

Run: `cd disosang; .\gradlew.bat bootRun`
브라우저에서 `http://localhost:8080` 접속 후 로그인.

- [ ] **Step 3: 상세페이지 검증**

- 가게 상세페이지 진입 → 폭이 홈/지도/내정보와 동일한 420px 프레임으로 보이는지 확인.
- `☆ 즐겨찾기` 클릭 → `★`(노랑) + 버튼 active 로 바뀌는지 확인.
- 새로고침 → `★` 상태 유지되는지 확인(서버 isFavorite 반영).
- 다시 클릭 → `☆`로 해제되는지 확인.

- [ ] **Step 4: 홈 화면 검증**

- 홈 진입 → 잔액/결제 카드와 결제 내역이 사라지고 즐겨찾기한 가게가 이름+별점+주소 카드로 보이는지 확인.
- 카드 클릭 → 해당 가게 상세페이지로 이동하는지 확인.
- 즐겨찾기를 모두 해제한 뒤 홈 진입 → "즐겨찾기한 가게가 없습니다" 안내 + 지도 링크가 보이는지 확인.

- [ ] **Step 5: 지도 검색 검증**

- 지도에서 검색 → 모든 결과 핀 위에 "★별점 + 가게이름"(또는 별점 없으면 이름만) 라벨이 보이는지 확인.
- 즐겨찾기한 가게가 검색 결과에 있으면 핀이 노란 별로 보이는지 확인.
- 핀 클릭 → 바텀시트에 가게 정보가 뜨고, 다시 클릭하면 상세로 이동하는지 확인(기존 동작 유지).

- [ ] **Step 6: 익명 사용자 검증**

- 로그아웃 상태에서 지도 검색 → 에러 없이 결과가 나오고 노란 별 핀이 없는지 확인(favorite=false).

- [ ] **Step 7: 최종 커밋(필요 시)**

검증 중 수정이 있었다면 커밋한다.

```bash
git add -A
git commit -m "test: verify favorites, map pins, and detail page width"
```

---

## Self-Review Notes

- **Spec 커버리지:** (1) 상세 크기 통일 → Task 6, (2) 핀 이름·별점 라벨 → Task 7, (3) 즐겨찾기 토글 + 홈 목록(결제 제거) → Task 1~6, 8, (4) 즐겨찾기 노란 별 핀 → Task 4, 5, 7. 모두 매핑됨.
- **기존 테스트 보호:** `StoreService` 시그니처를 바꾸지 않고 컨트롤러에서 favorite 마킹 → `StoreServiceFuzzySearchTest` 그대로 통과.
- **타입 일관성:** `StoreResponse.setFavorite`/`isFavorite`(boolean) ↔ `FavoriteService.getFavoriteStoreIds`(Set<Long>) ↔ `store.getId()`(Long) 일치. JS `data.favorite` ↔ 컨트롤러 `Map.of("favorite", boolean)` 일치.
- **보안:** `/favorites/**`는 SecurityConfig 기본 authenticated. `/store/map/search`는 permitAll 유지, user==null 분기 처리.
- **CSRF:** Spring Security 기본 CSRF 활성 → detail.html meta + fetch 헤더로 처리.
