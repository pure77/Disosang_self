const mapContainer = document.getElementById('map');
const mapOption = { center: new kakao.maps.LatLng(36.8, 127.1), level: 5 };
const map = new kakao.maps.Map(mapContainer, mapOption);
let markers = [];

let currentStores = [];
let currentPage = 1;
const pageSize = 10;

let infowindow = new kakao.maps.InfoWindow({zIndex:1});

// 인포윈도우를 닫는 함수
function closeInfoWindow() {
    infowindow.close();
}

// 지도를 클릭하면 인포윈도우를 닫습니다
kakao.maps.event.addListener(map, 'click', closeInfoWindow);

// 검색 폼 제출 이벤트
document.getElementById('searchForm').addEventListener('submit', function(e){
    e.preventDefault();
    const keyword = document.getElementById('keyword').value;
    doSearch(keyword);
});

// 카테고리 버튼 클릭 (HTML의 onclick에서 호출됨)
function searchCategory(category){
    document.getElementById('keyword').value = category;
    doSearch(category);
}

// 서버에 검색 요청
function doSearch(keyword){
    const bounds = map.getBounds();
    const sw = bounds.getSouthWest();
    const ne = bounds.getNorthEast();
    const center = map.getCenter();

    const params = new URLSearchParams({
        keyword: keyword,
        centerX: center.getLng(),
        centerY: center.getLat(),
        minX: sw.getLng(),
        minY: sw.getLat(),
        maxX: ne.getLng(),
        maxY: ne.getLat()
    });

    fetch('/store/map/search?' + params.toString())
        .then(res => res.json())
        .then(data => {
            //
            // [수정된 부분 1] : 테스트용 임시 데이터 생성 로직 제거
            //
            // const testData = data.map(store => ({ ... })); <-- 이 블록 전체 제거

            // 서버에서 받은 실제 데이터를 currentStores에 할당합니다.
            // 이제 서버 응답에 'rating', 'reviewCount' 등이 포함되어 있어야 합니다.
            currentStores = data;

            currentPage = 1;
            closeInfoWindow();
            showStores();
        });
}

// 현재 페이지의 검색 결과 표시
function showStores(){
    markers.forEach(m => m.setMap(null));
    markers = [];

    const listDiv = document.getElementById('storeList');
    listDiv.innerHTML = '';

    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    const stores = currentStores.slice(start, end);

    stores.forEach((store) => {
        const position = new kakao.maps.LatLng(store.y, store.x);
        const marker = new kakao.maps.Marker({ map: map, position: position });
        markers.push(marker);

        const content = createInfoWindowContent(store);

        kakao.maps.event.addListener(marker, 'click', function() {
        console.log("마커 클릭 / 가게 데이터:", store);
            infowindow.setContent(content);
            infowindow.open(map, marker);
        });

        const item = document.createElement('div');
        item.className = 'store-item';
        item.innerHTML = `<strong>${store.placeName}</strong><br><small>${store.addressName}</small>`;
        item.addEventListener('click', () => {
            map.setCenter(position);
            map.setLevel(3);
            infowindow.setContent(content);
            infowindow.open(map, marker);
        });
        listDiv.appendChild(item);
    });

    renderPagination();
}

//
// [수정된 부분 2] : 별점/리뷰가 없을 경우를 처리하도록 함수 수정
//
function createInfoWindowContent(store) {
    console.log("클릭한 가게 데이터:", store);
    // 별점 및 리뷰 HTML을 동적으로 생성
    let ratingHtml = '';
    // store.averageRating이 0보다 큰 유효한 값일 때만 별점/리뷰를 표시
    if (store.averageRating && store.averageRating > 0) {
        const stars = '★'.repeat(Math.floor(store.averageRating)) + '☆'.repeat(5 - Math.floor(store.averageRating));

        // ratingCount와 reviewCount도 유효한 경우에만 표시
        const ratingCountText = (store.averageRatingCount && store.averageRatingCount > 0)
                              ? `<span class="count">(${store.averageRatingCount}건)</span>`
                              : '';
        const reviewText = (store.reviewCount && store.reviewCount > 0)
                         ? `<span>리뷰 ${store.reviewCount}</span>`
                         : '';

        ratingHtml = `
        <div class="rating">
            <span class="star">${stars}</span>
            <strong>${store.averageRating}</strong>
            ${ratingCountText}
            ${reviewText}
        </div>`;
    } else {
        // 별점/리뷰 정보가 없는 경우
        ratingHtml = `
        <div class="rating">
            <span class="no-rating" style="color: #888; font-size: 13px;">별점/리뷰 정보가 없습니다.</span>
        </div>`;
    }

    const directionsUrl = `https://map.kakao.com/link/to/${store.placeName},${store.y},${store.x}`;
    const detailUrl = `/store/detail/${store.id}`;

    return `
    <div class="infowindow-wrap">
        <div class="close-btn" onclick="closeInfoWindow()">×</div>

        <div class="info-header">
            <div class="text-content">
                <a href="${detailUrl}" style="text-decoration: none; color: inherit;">
                    <div class="title">${store.placeName}</div>
                </a>
                ${ratingHtml}
            </div>

            ${store.thumbnailUrl ?
                `<a href="${detailUrl}">
                    <img src="${store.thumbnailUrl}" alt="${store.placeName}" class="thumbnail">
                 </a>` : ''}
        </div>

        <div class="info-body">
            <p>${store.roadAddressName || store.addressName}</p>
            <p class="jibun">(지번) ${store.addressName}</p>
            <p>${store.phone || '전화번호 정보 없음'}</p>
        </div>

        <div class="info-links">
            <a href="${store.placeUrl || '#'}" target="_blank">상세보기</a>
            <a href="#">정보 수정 제안</a>
            <a href="#">홈페이지</a>
        </div>

        <div class="info-buttons">
            <a href="#">&#x1F516; 저장</a>
            <a href="#">&#x1F4CD; 위치</a>
            <a href="${directionsUrl}" target="_blank" class="directions">길찾기</a>
            </div>
    </div>
    `;
}

// 페이지네이션 UI 렌더링
function renderPagination(){
    const totalPages = Math.ceil(currentStores.length / pageSize);
    const paginationDiv = document.getElementById('pagination');
    paginationDiv.innerHTML = '';

    for(let i=1; i<=totalPages; i++){
        const btn = document.createElement('button');
        btn.textContent = i;
        if(i === currentPage) btn.classList.add('active');
        btn.addEventListener('click', () => {
            currentPage = i;
            showStores();
        });
        paginationDiv.appendChild(btn);
    }
}

// 현재 위치 버튼 이벤트
document.getElementById('locBtn').addEventListener('click', function(){
    if(navigator.geolocation){
        navigator.geolocation.getCurrentPosition(function(pos){
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
        alert("GPS를 지원하지 않는 브라우저입니다.");
    }
});