import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

// 테스트 종료 후 실행되는 함수
export function handleSummary(data) {
  return {
    "load_test/summary.html": htmlReport(data), // 프로젝트 루트에 summary.html 생성
    stdout: textSummary(data, { indent: " ", enableColors: true }),
  };
}

export const options = {
  stages: [
    { duration: '30s', target: 20 }, // 30초 동안 사용자를 20명까지 서서히 올림 (Ramp-up)
    { duration: '1m', target: 20 },  // 1분 동안 20명 유지 (Load test)
    { duration: '10s', target: 0 },  // 10초 동안 사용자 종료 (Ramp-down)
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const BASE_URL = 'http://localhost:8080';

  // 1. StoreSearchRequest.java의 @Schema example 값을 기반으로 파라미터 구성
  const params = {
    keyword: '카페',                   // @NotBlank
    centerX: 127.1124,      // 중심 경도
    centerY: 36.8066,       // 중심 위도
    minX: 127.092241,         // 최소 경도
    maxX: 127.133911,         // 최대 경도
    minY: 36.798728,         // 최소 위도
    maxY: 36.813365,           // 최대 위도
  };

  // 2. 쿼리 스트링 변환
  const queryString = Object.keys(params)
    .map(key => `${key}=${encodeURIComponent(params[key])}`)
    .join('&');

  // 3. API 호출
  const res = http.get(`${BASE_URL}/store/map/search?${queryString}`);

  // 4. 결과 검증
  const checkResult = check(res, {
    'status is 200': (r) => r.status === 200,
    'body has valid json': (r) => {
      try {
        return Array.isArray(JSON.parse(r.body));
      } catch (e) {
        return false;
      }
    },
  });

  // 만약 실패한다면 로그를 찍어 원인 파악 (첫 번째 반복에서만 출력)
  if (!checkResult && __ITER === 0) {
    console.warn(`Test failed! Status: ${res.status}, Response: ${res.body}`);
  }

  sleep(1);
}