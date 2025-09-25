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
            // 테스트용 임시 데이터 추가
            const testData = data.map(store => ({
                ...store,
                rating: (Math.random() * 2 + 3).toFixed(1),
                ratingCount: Math.floor(Math.random() * 50),
                reviewCount: Math.floor(Math.random() * 100),
                phone: '070-8822-2211',
                placeUrl: `https://map.kakao.com/link/map/${store.id}`,
                thumbnailUrl: 'https://via.placeholder.com/70'
            }));
            currentStores = testData;

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

// 인포윈도우 HTML 콘텐츠 생성
function createInfoWindowContent(store) {
    const stars = '★'.repeat(Math.floor(store.rating)) + '☆'.repeat(5 - Math.floor(store.rating));
    const directionsUrl = `https://map.kakao.com/link/to/${store.placeName},${store.y},${store.x}`;

    return `
    <div class="infowindow-wrap">
        <div class="close-btn" onclick="closeInfoWindow()">×</div>
        <div class="info-header">
            <div>
                <div class="title">${store.placeName}</div>
                <div class="rating">
                    <span class="star">${stars}</span>
                    <strong>${store.rating}</strong>
                    <span class="count">(${store.ratingCount}건)</span>
                    <span>리뷰 ${store.reviewCount}</span>
                </div>
            </div>
            ${store.thumbnailUrl ? `<img src="${store.thumbnailUrl}" alt="${store.placeName}" class="thumbnail">` : ''}
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
            <a href="#">&#x1F517; 공유</a>
            <a href="${directionsUrl}" target="_blank" style="background-color: #4285f4; color: white; font-weight: bold;">길찾기</a>
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