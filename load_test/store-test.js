import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
// 테스트 설정
export const options = {
  scenarios: {
    // 1. 점진적 부하 증가 (Ramping Virtual Users)
    ramping_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 }, // 30초 동안 100명까지 증가
        { duration: '1m', target: 100 },  // 1분간 유지 (High Load)
        { duration: '30s', target: 0 },  // 종료
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    // 95%의 요청이 300ms 이내에 처리되어야 함 (엄격한 기준 설정)
    http_req_duration: ['p(95)<300'],
    // 에러율은 1% 미만이어야 함
    http_req_failed: ['rate<0.01'],
  },
};

// 검색어 목록 (데이터 분포에 따라 다양하게 구성)
const KEYWORDS = [
  '카페', '음식', '약국', '편의점', '이디야',
  '병원', '마트', '식당', '김밥', '은행'
];

// 기준 좌표 (천안/아산 부근으로 추정됨 - 예시 좌표 기반)
const BASE_LAT = 36.8066;
const BASE_LNG = 127.1124;

// 랜덤한 범위의 좌표를 생성하는 함수 (지도를 조금씩 움직이는 효과)
function getRandomCoordinate(base, variance) {
  return base + (Math.random() * variance * 2 - variance);
}

export default function () {
  const BASE_URL = 'http://localhost:8080'; // 실제 서버 주소

  // 1. 랜덤 파라미터 생성
  // 중심 좌표를 랜덤하게 약간씩 이동 (약 1~2km 반경 내 변동)
  const currentLat = getRandomCoordinate(BASE_LAT, 0.01);
  const currentLng = getRandomCoordinate(BASE_LNG, 0.01);

  // 뷰포트 크기 (화면 범위)
  const delta = 0.005;

  const params = {
    keyword: randomItem(KEYWORDS), // 랜덤 키워드 선택
    centerLat: currentLat,
    centerLng: currentLng,
    minLat: currentLat - delta,
    maxLat: currentLat + delta,
    minLng: currentLng - delta,
    maxLng: currentLng + delta,
    // categoryIds: [1, 2] // 필요하다면 카테고리 ID도 랜덤하게 추가 가능
  };

  // 2. 쿼리 스트링 조립
  const queryString = Object.keys(params)
    .map(key => `${key}=${encodeURIComponent(params[key])}`)
    .join('&');

  // 3. API 호출
  // 태그를 달아서 결과 리포트에서 구별하기 쉽게 함
  const res = http.get(`${BASE_URL}/store/map/search?${queryString}`, {
    tags: { name: 'SearchMap' },
  });

  // 4. 검증
  check(res, {
    'status is 200': (r) => r.status === 200,
    'duration < 500ms': (r) => r.timings.duration < 500,
  });

  // 사용자 Think Time (0.5초 ~ 1.5초 사이 랜덤 대기)
  sleep(Math.random() * 1 + 0.5);
}
// 이 부분을 스크립트 맨 아래에 추가하세요
export function handleSummary(data) {
  return {
    "./load_test/summary_FTS.html": htmlReport(data), // load_test 폴더 안에 summary.html로 저장
  };
}