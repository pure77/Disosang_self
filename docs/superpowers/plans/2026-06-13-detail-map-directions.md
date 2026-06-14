# 상세→지도 포커싱 & 현재위치 기반 길찾기 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 상세 화면의 지도/도착 버튼과 지도 화면의 길찾기·현재위치 동작을 현재위치 기반으로 동작하게 만든다.

**Architecture:** 백엔드는 단일 가게를 JSON으로 주는 엔드포인트 하나를 추가해 지도 화면이 기존 시트 렌더링을 재사용하게 한다. 프론트는 카카오맵 길찾기 호출을 공용 파일(`directions.js`)로 분리해 상세·지도 양쪽이 같은 현재위치 폴백 로직을 공유한다. 지도의 📍 버튼은 누락된 에러 콜백을 추가해 기본 좌표에 머무는 문제를 고친다.

**Tech Stack:** Spring Boot(Java) + Thymeleaf, Kakao Maps JS SDK, 바닐라 JS. 테스트는 JUnit5 + Mockito + AssertJ(컨트롤러 단위 테스트). 빌드는 Gradle(`disosang/`).

---

## File Structure

- `disosang/src/main/java/com/pmh/disosang/map/store/controller/StoreController.java` — 단일 가게 JSON 엔드포인트 추가 (수정)
- `disosang/src/test/java/com/pmh/disosang/map/store/controller/StoreControllerTest.java` — 컨트롤러 단위 테스트 (생성)
- `disosang/src/main/resources/static/js/directions.js` — 공용 카카오 길찾기 헬퍼 (생성)
- `disosang/src/main/resources/templates/store/detail.html` — 지도 버튼 링크화, 공유 삭제, 도착 data 속성, directions.js 로드 (수정)
- `disosang/src/main/resources/static/js/detail.js` — 도착 버튼 핸들러 (수정)
- `disosang/src/main/resources/templates/store/map.html` — directions.js 로드 (수정)
- `disosang/src/main/resources/static/js/map.js` — storeId URL 처리, 길찾기 핸들러, locBtn 에러 처리 (수정)

> 참고: 프론트(JS/HTML)는 이 저장소에 테스트 하네스가 없으므로 수동 검증으로 확인한다. 백엔드만 TDD로 진행한다.
> Gradle 명령은 `disosang/` 디렉터리에서 실행한다. (Windows는 `.\gradlew.bat`, 그 외 `./gradlew`)

---

### Task 1: 단일 가게 JSON 엔드포인트 (백엔드, TDD)

**Files:**
- Modify: `disosang/src/main/java/com/pmh/disosang/map/store/controller/StoreController.java`
- Test: `disosang/src/test/java/com/pmh/disosang/map/store/controller/StoreControllerTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`disosang/src/test/java/com/pmh/disosang/map/store/controller/StoreControllerTest.java` 생성:

```java
package com.pmh.disosang.map.store.controller;

import com.pmh.disosang.favorite.service.FavoriteService;
import com.pmh.disosang.map.store.dto.response.StoreResponse;
import com.pmh.disosang.map.store.service.StoreService;
import com.pmh.disosang.review.service.ReviewService;
import com.pmh.disosang.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock
    private StoreService storeService;
    @Mock
    private ReviewService reviewService;
    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private StoreController storeController;

    private StoreResponse storeWithId(Long id) {
        return StoreResponse.builder().id(id).placeName("가게").favorite(false).build();
    }

    @Test
    void 단일_가게_JSON은_로그인_사용자의_즐겨찾기를_표시한다() {
        given(storeService.findById(10L)).willReturn(storeWithId(10L));
        User user = User.builder().id(1L).name("u").email("u@e.com").password("p").build();
        given(favoriteService.isFavorite(1L, 10L)).willReturn(true);

        ResponseEntity<StoreResponse> res = storeController.storeDetailJson(10L, user);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isFavorite()).isTrue();
    }

    @Test
    void 익명_사용자는_즐겨찾기가_false이고_FavoriteService를_호출하지_않는다() {
        given(storeService.findById(10L)).willReturn(storeWithId(10L));

        ResponseEntity<StoreResponse> res = storeController.storeDetailJson(10L, null);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().isFavorite()).isFalse();
        verifyNoInteractions(favoriteService);
    }
}
```

- [ ] **Step 2: 테스트가 컴파일 실패(메서드 없음)하는지 확인**

Run: `./gradlew test --tests "com.pmh.disosang.map.store.controller.StoreControllerTest"`
Expected: 컴파일 실패 — `storeDetailJson` 심볼을 찾을 수 없음.

- [ ] **Step 3: 컨트롤러에 엔드포인트 추가**

`StoreController.java`의 import 블록에 다음을 추가:

```java
import org.springframework.web.bind.annotation.ResponseBody;
```

`storeDetail` 메서드(`@GetMapping("/detail/{storeId}")`) 바로 위에 다음 메서드를 추가:

```java
    @GetMapping("/detail/{storeId}/json")
    @ResponseBody
    public ResponseEntity<StoreResponse> storeDetailJson(@PathVariable("storeId") Long storeId,
                                                         @AuthenticationPrincipal User user) {
        StoreResponse storeInfo = storeService.findById(storeId);
        if (user != null) {
            storeInfo.setFavorite(favoriteService.isFavorite(user.getId(), storeId));
        }
        return ResponseEntity.ok(storeInfo);
    }
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.pmh.disosang.map.store.controller.StoreControllerTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add disosang/src/main/java/com/pmh/disosang/map/store/controller/StoreController.java \
        disosang/src/test/java/com/pmh/disosang/map/store/controller/StoreControllerTest.java
git commit -m "feat(store): add single-store JSON endpoint for map focus"
```

---

### Task 2: 공용 길찾기 헬퍼 `directions.js`

**Files:**
- Create: `disosang/src/main/resources/static/js/directions.js`

- [ ] **Step 1: `directions.js` 작성**

`disosang/src/main/resources/static/js/directions.js` 생성:

```javascript
// 카카오맵 길찾기를 "현재위치(출발) + 도착지"로 연다.
// 현재위치를 못 가져오면(권한 거부/비-HTTPS/미지원) 도착지만 넣어 연다(폴백).
function openKakaoDirections(name, lat, lng) {
    const dest = encodeURIComponent(name) + ',' + lat + ',' + lng;
    const toOnly = 'https://map.kakao.com/link/to/' + dest;

    function openDestinationOnly() {
        alert('현재위치를 가져올 수 없어 도착지만 표시합니다.');
        window.open(toOnly, '_blank');
    }

    if (!navigator.geolocation) {
        openDestinationOnly();
        return;
    }

    navigator.geolocation.getCurrentPosition(
        function (pos) {
            const from = encodeURIComponent('현재위치') + ',' + pos.coords.latitude + ',' + pos.coords.longitude;
            const url = 'https://map.kakao.com/link/from/' + from + '/to/' + dest;
            window.open(url, '_blank');
        },
        function () {
            openDestinationOnly();
        },
        { enableHighAccuracy: true, timeout: 8000 }
    );
}
```

- [ ] **Step 2: 커밋**

```bash
git add disosang/src/main/resources/static/js/directions.js
git commit -m "feat(js): add shared openKakaoDirections helper with current-location fallback"
```

---

### Task 3: 상세 화면 — 지도 링크화 / 공유 삭제 / 도착 버튼

**Files:**
- Modify: `disosang/src/main/resources/templates/store/detail.html`
- Modify: `disosang/src/main/resources/static/js/detail.js`

- [ ] **Step 1: action-buttons 영역 교체**

`detail.html`의 `<div class="action-buttons">` 블록(현재 4개 버튼)을 다음으로 교체:

```html
    <div class="action-buttons">
        <a th:href="@{/store/map(storeId=${store.id})}" class="btn"><span>🗺️</span> 지도</a>
        <button type="button" id="favoriteBtn" class="btn favorite-btn"
                th:classappend="${isFavorite} ? ' active' : ''"
                th:data-store-id="${store.id}">
            <span class="fav-icon" th:text="${isFavorite} ? '★' : '☆'">☆</span> 즐겨찾기
        </button>
        <a href="#" class="btn primary" id="arriveBtn"
           th:data-name="${store.placeName}"
           th:data-x="${store.x}"
           th:data-y="${store.y}"><span>🚗</span> 도착</a>
    </div>
```

(지도 버튼이 링크로 바뀌고, 공유 `<a>`는 삭제됐으며, 도착 버튼에 id와 data 속성이 추가됨.)

- [ ] **Step 2: detail.html에 directions.js 로드 추가**

`detail.html` 하단의 `<script src="/js/detail.js" defer></script>` 줄 **바로 앞**에 추가:

```html
<script src="/js/directions.js" defer></script>
```

- [ ] **Step 3: detail.js에 도착 버튼 핸들러 추가**

`detail.js` 파일 **맨 끝**에 다음 블록을 추가:

```javascript
// =========================================
// 도착 버튼 - 현재위치 기반 카카오맵 길찾기
// =========================================
document.addEventListener('DOMContentLoaded', function () {
    const arriveBtn = document.getElementById('arriveBtn');
    if (!arriveBtn) return;

    arriveBtn.addEventListener('click', function (e) {
        e.preventDefault();
        const name = arriveBtn.getAttribute('data-name');
        const lat = arriveBtn.getAttribute('data-y'); // y = 위도
        const lng = arriveBtn.getAttribute('data-x'); // x = 경도
        openKakaoDirections(name, lat, lng);
    });
});
```

- [ ] **Step 4: 빌드가 깨지지 않는지 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL (정적 리소스/템플릿 변경은 컴파일에 영향 없음 — 회귀 확인용)

- [ ] **Step 5: 커밋**

```bash
git add disosang/src/main/resources/templates/store/detail.html \
        disosang/src/main/resources/static/js/detail.js
git commit -m "feat(detail): map button links to map focus, remove share, add arrive directions"
```

---

### Task 4: 지도 화면 — 길찾기 핸들러 / storeId 포커싱 / locBtn 에러 처리

**Files:**
- Modify: `disosang/src/main/resources/templates/store/map.html`
- Modify: `disosang/src/main/resources/static/js/map.js`

- [ ] **Step 1: map.html에 directions.js 로드 추가**

`map.html` 하단의 `<script src="/js/map.js"></script>` 줄 **바로 앞**에 추가:

```html
<script src="/js/directions.js"></script>
```

- [ ] **Step 2: 길찾기 링크를 핸들러 연결용 앵커로 변경**

`map.js`의 `createInfoWindowContent` 함수에서 다음 줄을 삭제:

```javascript
    const directionsUrl = `https://map.kakao.com/link/to/${encodeURIComponent(store.placeName)},${store.y},${store.x}`;
```

그리고 같은 함수의 반환 템플릿 안 `info-buttons` 블록을 다음으로 교체:

```javascript
        <div class="info-buttons">
            <a href="#" class="directions">길찾기</a>
        </div>
```

- [ ] **Step 3: 시트에서 길찾기 클릭 핸들러 연결**

`map.js`의 `showStoreInfoInSheet(store)` 함수에서, 기존 `sheetInfoEl.querySelectorAll('a')` 루프 **바로 다음**에 추가:

```javascript
    const directionsLink = sheetInfoEl.querySelector('.directions');
    if (directionsLink) {
        directionsLink.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            openKakaoDirections(store.placeName, store.y, store.x);
        });
    }
```

- [ ] **Step 4: locBtn 에러 콜백 + 옵션 추가**

`map.js`의 `document.getElementById('locBtn').addEventListener(...)` 안에서
`navigator.geolocation.getCurrentPosition(function (pos) { ... });` 호출을 다음으로 교체
(성공 콜백 본문은 그대로 두고, 에러 콜백과 옵션만 추가):

```javascript
        navigator.geolocation.getCurrentPosition(function (pos) {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            const locPosition = new kakao.maps.LatLng(lat, lng);

            map.setCenter(locPosition);
            map.setLevel(4);

            const marker = new kakao.maps.Marker({
                position: locPosition,
                map: map
            });
            markers.push(marker);

            infowindow.setContent("<div style='padding:5px;'>현재 위치</div>");
            infowindow.open(map, marker);
        }, function (err) {
            if (err.code === err.PERMISSION_DENIED) {
                alert('위치 권한이 거부되었습니다. 브라우저 설정에서 위치 접근을 허용해주세요.');
            } else {
                alert('현재위치를 가져올 수 없습니다. (HTTPS 환경에서만 동작합니다)');
            }
        }, { enableHighAccuracy: true, timeout: 8000 });
```

- [ ] **Step 5: storeId 쿼리 포커싱 추가**

`map.js` 파일 **맨 끝**에 다음 블록을 추가:

```javascript
// 상세 화면의 "지도" 버튼으로 진입한 경우: 해당 가게 핀으로 이동 + 정보시트 자동 열기
(function focusStoreFromQuery() {
    const params = new URLSearchParams(window.location.search);
    const storeId = params.get('storeId');
    if (!storeId) return;

    fetch('/store/detail/' + storeId + '/json')
        .then(function (res) {
            if (!res.ok) throw new Error('가게 정보를 불러오지 못했습니다.');
            return res.json();
        })
        .then(function (store) {
            currentStores = [store];
            currentPage = 1;
            showStores();

            const position = new kakao.maps.LatLng(store.y, store.x);
            map.setCenter(position);
            map.setLevel(4);
            showStoreInfoInSheet(store);
        })
        .catch(function () {
            renderStatus('가게 정보를 불러오지 못했습니다.', true);
        });
})();
```

- [ ] **Step 6: 빌드 회귀 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add disosang/src/main/resources/templates/store/map.html \
        disosang/src/main/resources/static/js/map.js
git commit -m "feat(map): current-location directions, storeId focus, locBtn error handling"
```

---

### Task 5: 통합 수동 검증

**Files:** (없음 — 실행/검증만)

- [ ] **Step 1: 전체 테스트 실행**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL (기존 테스트 + StoreControllerTest 모두 통과)

- [ ] **Step 2: 앱 실행 후 브라우저 수동 확인**

앱을 `localhost`에서 실행한다(geolocation은 localhost/HTTPS에서만 동작).
다음을 순서대로 확인:

1. 가게 상세 → **지도** 클릭 → `/store/map?storeId=...`로 이동, 지도가 해당 가게로 중심 이동하고 하단 정보시트가 자동으로 열림.
2. 상세 화면에 **공유** 버튼이 더 이상 보이지 않음.
3. 상세 → **도착** 클릭 → 위치 권한 허용 시 카카오맵이 `출발=현재위치, 도착=가게`로 열림. 거부 시 도착지만 열리고 안내 alert 표시.
4. 지도 시트 → **길찾기** 클릭 → 위 3과 동일하게 현재위치가 출발지로 들어감.
5. 지도 **📍** 클릭 → 실제 현재위치로 이동(기본 좌표에 머물지 않음). 권한 거부 시 안내 alert 표시.

- [ ] **Step 3: 검증 완료 후 마무리**

모든 항목이 통과하면 이 작업 묶음 완료. (별도 커밋 없음 — 코드 변경 없는 검증 단계)

---

## 비고 / 제약

- `navigator.geolocation`은 **HTTPS 또는 localhost**에서만 동작한다. 운영이 HTTP면 도착/길찾기는 도착지-only 폴백, 📍는 안내 alert로 동작한다.
- `build/`, `out/` 아래의 동일 파일들은 빌드 산출물이므로 직접 수정하지 않는다(소스만 수정하면 빌드 시 갱신됨).
