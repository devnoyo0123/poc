import { check, group, sleep } from 'k6';
import { SharedArray } from 'k6/data';

const SERVLET_URL = __ENV.SERVLET_URL || 'http://localhost:8081';
const WEBFLUX_URL = __ENV.WEBFLUX_URL || 'http://localhost:8082';

// Custom metrics for SSE
const sseConnections = new SharedArray('sse_connections', function () {
    return [];
});

export const options = {
    scenarios: {
        servlet_ramp: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 100 },   // 100 connections
                { duration: '30s', target: 1000 },  // 1,000 connections
                { duration: '60s', target: 5000 },  // 5,000 connections
                { duration: '120s', target: 10000 },// 10,000 connections (C10K)
                { duration: '60s', target: 0 },     // Ramp down
            ],
            gracefulRampDown: '30s',
            exec: 'servletTest',
        },
        webflux_ramp: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 100 },   // 100 connections
                { duration: '30s', target: 1000 },  // 1,000 connections
                { duration: '60s', target: 5000 },  // 5,000 connections
                { duration: '120s', target: 10000 },// 10,000 connections (C10K)
                { duration: '60s', target: 0 },     // Ramp down
            ],
            gracefulRampDown: '30s',
            exec: 'webfluxTest',
            startTime: '5s', // Start after servlet
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export function servletTest() {
    const url = `${SERVLET_URL}/api/prices/stream`;
    testSSEConnection(url, 'servlet');
}

export function webfluxTest() {
    const url = `${WEBFLUX_URL}/api/prices/stream`;
    testSSEConnection(url, 'webflux');
}

function testSSEConnection(url, stackName) {
    const params = {
        headers: {
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache',
        },
        timeout: '300s', // 5 minutes
    };

    const res = http.get(url, params);

    check(res, {
        [`${stackName}: SSE connection established`]: (r) => r.status === 200,
        [`${stackName}: Content-Type is text/event-stream`]: (r) =>
            r.headers['Content-Type'].includes('text/event-stream'),
        [`${stackName}: Response time acceptable`]: (r) => r.timings.duration < 1000,
    });

    sleep(1);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'results/sse-summary.json': JSON.stringify(data),
        'results/sse-summary.html': htmlReport(data),
    };
}

function textSummary(data, options) {
    const summary = {
        'Servlet Stack': {
            'Total Requests': data.metrics.http_reqs.values.count,
            'Failed Requests': data.metrics.http_req_failed.values.passes || 0,
            'Avg Response Time': `${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`,
            'P95 Response Time': `${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`,
        },
        'WebFlux Stack': {
            'Total Requests': data.metrics.http_reqs.values.count,
            'Failed Requests': data.metrics.http_req_failed.values.passes || 0,
            'Avg Response Time': `${data.metrics.http_req_duration.values.avg.toFixed(2)}ms`,
            'P95 Response Time': `${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms`,
        },
    };

    return JSON.stringify(summary, null, 2);
}

function htmlReport(data) {
    return `
<!DOCTYPE html>
<html>
<head>
    <title>SSE Performance Test Results</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        tr:nth-child(even) { background-color: #f2f2f2; }
        .metric { margin: 10px 0; }
        .label { font-weight: bold; }
    </style>
</head>
<body>
    <h1>SSE Performance Test Results</h1>
    <p>Test completed at: ${new Date().toISOString()}</p>

    <h2>Summary</h2>
    <div class="metric">
        <span class="label">Total Requests:</span> ${data.metrics.http_reqs.values.count}
    </div>
    <div class="metric">
        <span class="label">Failed Requests:</span> ${data.metrics.http_req_failed.values.passes || 0}
    </div>
    <div class="metric">
        <span class="label">Avg Response Time:</span> ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms
    </div>
    <div class="metric">
        <span class="label">P95 Response Time:</span> ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
    </div>

    <h2>Metrics by Scenario</h2>
    <table>
        <tr>
            <th>Scenario</th>
            <th>Requests</th>
            <th>Failures</th>
            <th>Avg Time</th>
            <th>P95 Time</th>
        </tr>
        ${Object.entries(data.scenarios || {}).map(([name, scenario]) => `
        <tr>
            <td>${name}</td>
            <td>${scenario.metrics.http_reqs?.values.count || 0}</td>
            <td>${scenario.metrics.http_req_failed?.values.passes || 0}</td>
            <td>${scenario.metrics.http_req_duration?.values.avg?.toFixed(2) || 0}ms</td>
            <td>${scenario.metrics.http_req_duration?.values['p(95)']?.toFixed(2) || 0}ms</td>
        </tr>
        `).join('')}
    </table>
</body>
</html>
    `;
}
