# 상세→지도 포커싱 & 현재위치 기반 길찾기 설계

작성일: 2026-06-13

## 목표

가게 상세 화면과 지도 검색 화면의 위치/길찾기 동작을 개선한다.

1. 상세 화면의 **지도** 버튼 → 지도 검색 화면으로 이동해 해당 가게 핀으로 이동하고 정보시트를 자동으로 연다.
2. 상세 화면의 **공유** 버튼 → 아직 미구현이므로 삭제한다.
3. 상세 화면의 **도착** 버튼 → 카카오맵 길찾기를 **현재위치(출발) + 가게(도착)**로 연다.
4. 지도 시트의 **길찾기** 버튼 → 현재 도착지만 들어가는데, **현재위치(출발)**도 넣는다.
5. 지도의 **📍 버튼** → 현재 기본값(36.8, 127.1)에 머무는 문제를 고쳐 실제 현재위치를 찾게 한다.

## 현재 상태

- `detail.html:22` 지도 버튼 `href="#"` (동작 없음). 상세 페이지에는 `store.x`(경도), `store.y`(위도), `store.id`, `store.placeName`이 이미 모델에 있다.
- `detail.html:23` 공유 버튼 `href="#"`.
- `detail.html:29` 도착 버튼 `href="#"`.
- `map.js:449,501` 길찾기 링크가 `https://map.kakao.com/link/to/...` (도착지만).
- `map.js:526` 📍 `locBtn`은 `navigator.geolocation.getCurrentPosition`을 호출하지만 **에러 콜백이 없어** 권한 거부/비-HTTPS 시 조용히 실패하고 지도가 기본 중심에 머문다.
- 단일 가게를 JSON으로 주는 엔드포인트는 없다(`/store/detail/{id}`는 HTML 렌더링).

## 설계

### 1. 백엔드 — 단일 가게 JSON 엔드포인트

`StoreController`에 추가:

```
GET /store/detail/{storeId}/json  →  ResponseEntity<StoreResponse>
```

- 기존 `storeService.findById(storeId)` 재사용.
- 로그인 사용자면 `favoriteService.isFavorite`로 `favorite` 플래그를 채운다(기존 지도 검색과 동일 패턴).

### 2. detail.html

- **지도 버튼**: `href="#"` → `th:href="@{/store/map(storeId=${store.id})}"` (단순 링크, JS 불필요).
- **공유 버튼**: 해당 `<a>` 삭제.
- **도착 버튼**: `data-name` / `data-x` / `data-y` 속성을 추가하고, detail.js에서 클릭 핸들러를 연결한다.

### 3. 공용 길찾기 헬퍼 (신규 파일 `directions.js`)

detail.js와 map.js 양쪽에서 쓰므로 중복 제거를 위해 공용 파일로 분리한다.

```
openKakaoDirections(name, lat, lng):
  navigator.geolocation 성공 →
    https://map.kakao.com/link/from/내위치,curLat,curLng/to/{name},{lat},{lng}
  실패/미지원 →
    https://map.kakao.com/link/to/{name},{lat},{lng}   (도착지-only 폴백)
    + "현재위치를 가져올 수 없어 도착지만 표시합니다" 안내
  window.open(url, '_blank')
```

- 위치 옵션: `enableHighAccuracy`, 적절한 `timeout`.
- detail.html, map.html 양쪽에서 `directions.js`를 먼저 로드한다.

### 4. detail.js

- 도착 버튼 클릭 핸들러 추가 → 버튼의 `data-*`에서 name/x/y를 읽어 `openKakaoDirections` 호출.

### 5. map.js

- **URL `storeId` 처리**: 페이지 로드 시 `storeId` 파라미터가 있으면
  `/store/detail/{id}/json`을 fetch → `currentStores=[store]` → `showStores()` →
  `map.setCenter(가게좌표)` + `setLevel` + `showStoreInfoInSheet(store)`.
- **길찾기 버튼**: 시트 정보창의 `.directions`를 정적 anchor에서 클릭 핸들러로 변경,
  `openKakaoDirections(store.placeName, store.y, store.x)` 호출.
- **📍 locBtn 수정**: `getCurrentPosition`에 **에러 콜백 + 옵션**(`enableHighAccuracy`, `timeout`)을
  추가. 실패 시 alert로 사유(권한 거부 / 비-HTTPS 등) 안내. → "기본값" 문제 해결.

## 제약사항

- `navigator.geolocation`은 **HTTPS 또는 localhost에서만** 동작한다. 로컬 개발은 정상이나,
  운영 배포가 HTTP면 현재위치 기능 전체가 실패하며 이때 길찾기는 도착지-only 폴백으로 동작한다.

## 범위 밖 (YAGNI)

- 공유 기능 구현(삭제만 함).
- 지도상의 실시간 경로 표시(카카오맵 외부 링크로 위임).
- 현재위치 마커의 지속적 추적.
