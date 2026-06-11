const mapContainer = document.getElementById('map');
const mapOption = { center: new kakao.maps.LatLng(36.8, 127.1), level: 5 };
const map = new kakao.maps.Map(mapContainer, mapOption);
const placesService = new kakao.maps.services.Places();
const LOCATION_SUFFIXES = ['역', '동', '읍', '면', '리', '구', '시', '군', '로', '길', '번길', '터미널', '공원', '시장', '학교', '대학교', '병원'];

let markers = [];
let currentStores = [];
let currentPage = 1;
const pageSize = 10;

let infowindow = new kakao.maps.InfoWindow({ zIndex: 1 });
const markerImageCache = new Map();

const bottomSheet = document.getElementById('bottomSheet');
const sheetHandle = document.getElementById('sheetHandle');
const sheetListEl = document.getElementById('sheetList');
const sheetInfoEl = document.getElementById('sheetInfo');

// 시트 바닥을 실제 바텀 네비 높이 위에 고정 (--nav-h)
const navEl = document.querySelector('.bottom-nav');
if (navEl) {
    document.documentElement.style.setProperty('--nav-h', navEl.offsetHeight + 'px');
}

function setSheetState(state) {
    bottomSheet.classList.remove('hidden', 'expanded', 'half', 'collapsed');
    bottomSheet.style.height = '';
    bottomSheet.classList.add(state);
}

function showSheetList() {
    sheetInfoEl.hidden = true;
    sheetListEl.hidden = false;
}

function showStoreInfoInSheet(store) {
    sheetInfoEl.innerHTML = createInfoWindowContent(store);
    sheetListEl.hidden = true;
    sheetInfoEl.hidden = false;
    setSheetState('half');

    const closeBtn = sheetInfoEl.querySelector('.close-btn');
    if (closeBtn) {
        closeBtn.onclick = function (e) {
            e.stopPropagation();
            showSheetList();
        };
    }
    sheetInfoEl.querySelectorAll('a').forEach(function (el) {
        el.addEventListener('click', function (e) {
            e.stopPropagation();
        });
    });
    const wrap = sheetInfoEl.querySelector('.infowindow-wrap');
    if (wrap) {
        wrap.addEventListener('click', function () {
            window.location.href = '/store/detail/' + store.id;
        });
    }
}

function buildMarkerSvg(fillColor) {
    return `
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="42" viewBox="0 0 32 42">
            <path d="M16 1C8.3 1 2 7.3 2 15c0 10.4 11.8 22.6 13.1 23.9a1.3 1.3 0 0 0 1.8 0C18.2 37.6 30 25.4 30 15 30 7.3 23.7 1 16 1z"
                  fill="${fillColor}" stroke="#2c3e50" stroke-width="1.5"/>
            <circle cx="16" cy="15" r="5.5" fill="#ffffff"/>
        </svg>
    `.trim();
}

function getMarkerImage(fillColor) {
    if (!markerImageCache.has(fillColor)) {
        const svg = buildMarkerSvg(fillColor);
        const imageUrl = 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svg);

        markerImageCache.set(
            fillColor,
            new kakao.maps.MarkerImage(
                imageUrl,
                new kakao.maps.Size(32, 42),
                { offset: new kakao.maps.Point(16, 42) }
            )
        );
    }

    return markerImageCache.get(fillColor);
}

function getMarkerOptions(store, position) {
    const storeType = (store.storeType || '').toLowerCase();

    if (storeType === 'cheap') {
        return {
            map: map,
            position: position,
            image: getMarkerImage('#27ae60')
        };
    }

    if (storeType === 'tm') {
        return {
            map: map,
            position: position,
            image: getMarkerImage('#f39c12')
        };
    }

    return { map: map, position: position };
}

function closeInfoWindow() {
    infowindow.close();
}

kakao.maps.event.addListener(map, 'click', closeInfoWindow);

document.getElementById('searchForm').addEventListener('submit', function (e) {
    e.preventDefault();
    const keyword = normalizeKeyword(document.getElementById('keyword').value);
    doSearch(keyword);
});

function searchCategory(category) {
    document.getElementById('keyword').value = category;
    doSearch(category);
}

async function doSearch(rawKeyword) {
    const keyword = normalizeKeyword(rawKeyword);
    if (!keyword) {
        currentStores = [];
        currentPage = 1;
        closeInfoWindow();
        showStores();
        renderStatus('검색어를 입력해주세요.', true);
        return;
    }

    renderStatus('검색 중입니다...');

    try {
        const searchContext = await resolveSearchContext(keyword);
        const params = new URLSearchParams({
            keyword: searchContext.keyword,
            centerX: searchContext.bounds.centerX,
            centerY: searchContext.bounds.centerY,
            minX: searchContext.bounds.minX,
            minY: searchContext.bounds.minY,
            maxX: searchContext.bounds.maxX,
            maxY: searchContext.bounds.maxY
        });

        const res = await fetch('/store/map/search?' + params.toString());
        if (!res.ok) {
            const errorBody = await res.json().catch(() => null);
            throw new Error(errorBody?.message || '검색 요청에 실패했습니다.');
        }

        const data = await res.json();
        currentStores = Array.isArray(data) ? data : [];
        currentPage = 1;
        ensureResultsVisible(currentStores);
        closeInfoWindow();
        showStores();
        showSheetList();
        setSheetState(currentStores.length ? 'half' : 'collapsed');

        if (searchContext.notice) {
            renderStatus(searchContext.notice);
        } else if (currentStores.length === 0) {
            renderStatus('검색 결과가 없습니다.', true);
        } else {
            clearStatus();
        }
    } catch (error) {
        currentStores = [];
        currentPage = 1;
        closeInfoWindow();
        showStores();
        showSheetList();
        setSheetState('collapsed');
        renderStatus(error.message || '검색 중 오류가 발생했습니다.', true);
    }
}

function normalizeKeyword(keyword) {
    return (keyword || '').trim().replace(/\s+/g, ' ');
}

function getCurrentMapSearchBounds() {
    const bounds = map.getBounds();
    const sw = bounds.getSouthWest();
    const ne = bounds.getNorthEast();
    const center = map.getCenter();

    return {
        centerX: center.getLng(),
        centerY: center.getLat(),
        minX: sw.getLng(),
        minY: sw.getLat(),
        maxX: ne.getLng(),
        maxY: ne.getLat()
    };
}

function extractLocationIntent(keyword) {
    const tokens = keyword.split(' ');
    if (tokens.length < 2) {
        return null;
    }

    const searchKeyword = tokens[tokens.length - 1];
    const locationKeyword = tokens.slice(0, -1).join(' ');
    if (!locationKeyword || !searchKeyword) {
        return null;
    }

    const looksLikeLocation = LOCATION_SUFFIXES.some((suffix) =>
        locationKeyword.endsWith(suffix) || locationKeyword.includes(suffix)
    );

    if (!looksLikeLocation) {
        return null;
    }

    return { locationKeyword, searchKeyword };
}

function createBoundsAround(lat, lng, radiusMeters = 1500) {
    const latDelta = radiusMeters / 111320;
    const lngDelta = radiusMeters / (111320 * Math.max(Math.cos((lat * Math.PI) / 180), 0.2));

    return {
        centerX: lng,
        centerY: lat,
        minX: lng - lngDelta,
        minY: lat - latDelta,
        maxX: lng + lngDelta,
        maxY: lat + latDelta
    };
}

function resolvePlaceKeyword(keyword) {
    return new Promise((resolve) => {
        placesService.keywordSearch(keyword, (data, status) => {
            if (status === kakao.maps.services.Status.OK && data.length > 0) {
                resolve(data[0]);
                return;
            }

            resolve(null);
        }, { size: 1 });
    });
}

async function resolveSearchContext(keyword) {
    const locationIntent = extractLocationIntent(keyword);
    if (!locationIntent) {
        return {
            keyword,
            bounds: getCurrentMapSearchBounds(),
            notice: ''
        };
    }

    const place = await resolvePlaceKeyword(locationIntent.locationKeyword);
    if (!place) {
        return {
            keyword,
            bounds: getCurrentMapSearchBounds(),
            notice: `'${locationIntent.locationKeyword}' 위치를 찾지 못해 현재 지도 기준으로 검색했어요.`
        };
    }

    const lat = Number(place.y);
    const lng = Number(place.x);
    const bounds = createBoundsAround(lat, lng);
    const mapBounds = new kakao.maps.LatLngBounds(
        new kakao.maps.LatLng(bounds.minY, bounds.minX),
        new kakao.maps.LatLng(bounds.maxY, bounds.maxX)
    );

    map.setBounds(mapBounds);

    return {
        keyword: locationIntent.searchKeyword,
        bounds,
        notice: `${locationIntent.locationKeyword} 주변으로 검색했어요.`
    };
}

function getStatusElement() {
    let statusElement = document.getElementById('searchStatus');
    if (!statusElement) {
        statusElement = document.createElement('p');
        statusElement.id = 'searchStatus';
        statusElement.style.margin = '8px 0 12px';
        statusElement.style.fontSize = '13px';
        statusElement.style.color = '#666';

        const listView = document.getElementById('sheetList');
        const storeList = document.getElementById('storeList');
        listView.insertBefore(statusElement, storeList);
    }

    return statusElement;
}

function renderStatus(message, isError = false) {
    const statusElement = getStatusElement();
    statusElement.textContent = message;
    statusElement.style.color = isError ? '#c0392b' : '#666';
}

function clearStatus() {
    const statusElement = document.getElementById('searchStatus');
    if (statusElement) {
        statusElement.textContent = '';
    }
}

function ensureResultsVisible(stores) {
    if (!stores || stores.length === 0) {
        return;
    }

    const currentBounds = map.getBounds();
    const allOutside = stores.every((store) => !currentBounds.contain(new kakao.maps.LatLng(store.y, store.x)));
    if (!allOutside) {
        return;
    }

    const resultBounds = new kakao.maps.LatLngBounds();
    stores.forEach((store) => resultBounds.extend(new kakao.maps.LatLng(store.y, store.x)));
    map.setBounds(resultBounds);
}

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
        const marker = new kakao.maps.Marker(getMarkerOptions(store, position));
        markers.push(marker);

        kakao.maps.event.addListener(marker, 'click', function () {
            map.setCenter(position);
            showStoreInfoInSheet(store);
        });

        const item = document.createElement('div');
        item.className = 'store-item';
        item.innerHTML = `<strong>${store.placeName}</strong><br><small>${store.addressName}</small>`;
        item.addEventListener('click', () => {
            window.location.href = '/store/detail/' + store.id;
        });
        listDiv.appendChild(item);
    });

    renderPagination();
}

function createInfoWindowContent(store) {
    let ratingHtml = '';
    if (store.averageRating && store.averageRating > 0) {
        const stars = '★'.repeat(Math.floor(store.averageRating)) + '☆'.repeat(5 - Math.floor(store.averageRating));
        const reviewText = (store.reviewCount && store.reviewCount > 0)
            ? `<span>리뷰 ${store.reviewCount}</span>`
            : '';

        ratingHtml = `
        <div class="rating">
            <span class="star">${stars}</span>
            <strong>${store.averageRating}</strong>
            ${reviewText}
        </div>`;
    } else {
        ratingHtml = `
        <div class="rating">
            <span class="no-rating" style="color: #888; font-size: 13px;">별점/리뷰 정보가 없습니다.</span>
        </div>`;
    }

    const directionsUrl = `https://map.kakao.com/link/to/${store.placeName},${store.y},${store.x}`;
    const detailUrl = `/store/detail/${store.id}`;

    return `
    <div class="infowindow-wrap">
        <div class="close-btn">×</div>

        <div class="info-header">
            <div class="text-content">
                <a href="${detailUrl}" style="text-decoration: none; color: inherit;">
                    <div class="title">${store.placeName}</div>
                </a>
                ${ratingHtml}
            </div>

            ${store.thumbnailUrl
                ? `<a href="${detailUrl}">
                    <img src="${store.thumbnailUrl}" alt="${store.placeName}" class="thumbnail">
                 </a>`
                : ''}
        </div>

        <div class="info-body">
            <p>${store.roadAddressName || store.addressName}</p>
            <p class="jibun">(지번) ${store.addressName}</p>
            <p>${store.phone || '전화번호 정보 없음'}</p>
        </div>

        <div class="info-links">
            <a href="#">정보 수정 제안</a>
        </div>

        <div class="info-buttons">
            <a href="${directionsUrl}" target="_blank" class="directions">길찾기</a>
        </div>
    </div>
    `;
}

function renderPagination() {
    const totalPages = Math.ceil(currentStores.length / pageSize);
    const paginationDiv = document.getElementById('pagination');
    paginationDiv.innerHTML = '';

    for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement('button');
        btn.textContent = i;
        if (i === currentPage) {
            btn.classList.add('active');
        }
        btn.addEventListener('click', () => {
            currentPage = i;
            showStores();
        });
        paginationDiv.appendChild(btn);
    }
}

document.getElementById('locBtn').addEventListener('click', function () {
    if (navigator.geolocation) {
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
        });
    } else {
        alert('GPS를 지원하지 않는 브라우저입니다.');
    }
});

// 바텀시트 드래그(카카오맵식 3단 스냅) — 높이 기반(바닥은 네비 위에 고정)
function snapHeights() {
    return [
        { state: 'collapsed', h: 96 },
        { state: 'half', h: window.innerHeight * 0.42 },
        { state: 'expanded', h: window.innerHeight - 200 }
    ];
}

let sheetDragging = false;
let sheetStartY = 0;
let sheetStartHeight = 0;
let sheetCurrentHeight = 0;

sheetHandle.addEventListener('pointerdown', function (e) {
    sheetDragging = true;
    sheetStartY = e.clientY;
    sheetStartHeight = bottomSheet.getBoundingClientRect().height;
    sheetCurrentHeight = sheetStartHeight;
    bottomSheet.style.transition = 'none';
    sheetHandle.setPointerCapture(e.pointerId);
});

sheetHandle.addEventListener('pointermove', function (e) {
    if (!sheetDragging) return;
    const minH = 96;
    const maxH = window.innerHeight - 160;
    // 위로 끌면(clientY 감소) 시트 높이 증가
    sheetCurrentHeight = Math.min(Math.max(sheetStartHeight + (sheetStartY - e.clientY), minH), maxH);
    bottomSheet.style.height = sheetCurrentHeight + 'px';
});

sheetHandle.addEventListener('pointerup', function (e) {
    if (!sheetDragging) return;
    sheetDragging = false;
    bottomSheet.style.transition = '';
    sheetHandle.releasePointerCapture(e.pointerId);

    // 거의 안 움직였으면 탭으로 간주: collapsed <-> half 토글
    if (Math.abs(sheetCurrentHeight - sheetStartHeight) < 5) {
        const cur = snapHeights().find((s) => bottomSheet.classList.contains(s.state));
        setSheetState(cur && cur.state === 'collapsed' ? 'half' : 'collapsed');
        return;
    }

    // 가장 가까운 스냅 높이로 고정
    let nearest = snapHeights()[0];
    let best = Infinity;
    snapHeights().forEach(function (o) {
        const d = Math.abs(o.h - sheetCurrentHeight);
        if (d < best) {
            best = d;
            nearest = o;
        }
    });
    setSheetState(nearest.state);
});
