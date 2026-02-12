// 5,000 connections test (C5K)
import { check } from 'k6';
import http from 'k6/http';

const SERVLET_URL = __ENV.SERVLET_URL || 'http://localhost:8081';
const WEBFLUX_URL = __ENV.WEBFLUX_URL || 'http://localhost:8082';

export const options = {
    vus: 5000,
    duration: '2m',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const urls = [
        `${SERVLET_URL}/api/prices/stream`,
        `${WEBFLUX_URL}/api/prices/stream`,
    ];

    urls.forEach(url => {
        const stackName = url.includes('8081') ? 'servlet' : 'webflux';

        const res = http.get(url, {
            headers: { 'Accept': 'text/event-stream' },
            timeout: '120s',
        });

        check(res, {
            [`${stackName}: status 200`]: (r) => r.status === 200,
            [`${stackName}: SSE content type`]: (r) =>
                r.headers['Content-Type']?.includes('text/event-stream'),
        });
    });
}
