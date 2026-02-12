// 10,000 connections test (C10K) - Main Performance Comparison
import { check } from 'k6';
import http from 'k6/http';

const SERVLET_URL = __ENV.SERVLET_URL || 'http://localhost:8081';
const WEBFLUX_URL = __ENV.WEBFLUX_URL || 'http://localhost:8082';

// Test C10K - This is where WebFlux should shine with fewer threads!
export const options = {
    scenarios: {
        servlet_c10k: {
            executor: 'constant-vus',
            vus: 10000,
            duration: '2m',
            gracefulStop: '30s',
            exec: 'servletTest',
        },
        webflux_c10k: {
            executor: 'constant-vus',
            vus: 10000,
            duration: '2m',
            gracefulStop: '30s',
            startTime: '10s', // Start after servlet
            exec: 'webfluxTest',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.05'], // Allow 5% failure rate at C10K
    },
};

export function servletTest() {
    const url = `${SERVLET_URL}/api/prices/stream`;

    const res = http.get(url, {
        headers: {
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache',
        },
        timeout: '120s',
    });

    check(res, {
        'servlet: status 200': (r) => r.status === 200,
        'servlet: SSE content type': (r) =>
            r.headers['Content-Type']?.includes('text/event-stream'),
    });
}

export function webfluxTest() {
    const url = `${WEBFLUX_URL}/api/prices/stream`;

    const res = http.get(url, {
        headers: {
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache',
        },
        timeout: '120s',
    });

    check(res, {
        'webflux: status 200': (r) => r.status === 200,
        'webflux: SSE content type': (r) =>
            r.headers['Content-Type']?.includes('text/event-stream'),
    });
}

export function handleSummary(data) {
    return {
        'stdout': JSON.stringify({
            'C10K Test Results': {
                'Servlet': {
                    'Total Requests': data.scenarios?.servlet_c10k?.metrics?.http_reqs?.values?.count || 0,
                    'Failed Requests': data.scenarios?.servlet_c10k?.metrics?.http_req_failed?.values?.passes || 0,
                    'Avg Response Time': data.scenarios?.servlet_c10k?.metrics?.http_req_duration?.values?.avg?.toFixed(2) + 'ms',
                    'P95 Response Time': data.scenarios?.servlet_c10k?.metrics?.http_req_duration?.values?.['p(95)']?.toFixed(2) + 'ms',
                },
                'WebFlux': {
                    'Total Requests': data.scenarios?.webflux_c10k?.metrics?.http_reqs?.values?.count || 0,
                    'Failed Requests': data.scenarios?.webflux_c10k?.metrics?.http_req_failed?.values?.passes || 0,
                    'Avg Response Time': data.scenarios?.webflux_c10k?.metrics?.http_req_duration?.values?.avg?.toFixed(2) + 'ms',
                    'P95 Response Time': data.scenarios?.webflux_c10k?.metrics?.http_req_duration?.values?.['p(95)']?.toFixed(2) + 'ms',
                },
            },
        }, null, 2),
        'results/c10k-results.json': JSON.stringify(data),
    };
}
