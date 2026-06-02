/**
 * k6 rate limit 검증 스크립트
 *
 * 실행: k6 run k6-script.js
 *
 * 60초 동안 무분별한 호출. 검증은 테스트 후:
 *   curl http://localhost:8090/api/stats?seconds=120
 *   → rateLimitOk: true, maxCallsPerSecond <= 2
 */
import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '60s',
};

const BASE = __ENV.BASE_URL || 'http://localhost:8090';

export default function () {
  http.post(
    `${BASE}/api/process`,
    JSON.stringify({ payload: '' }),
    { headers: { 'Content-Type': 'application/json' }, timeout: '65s' },
  );
  sleep(0.05); // 50ms 간격으로 무분별 호출
}
