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
