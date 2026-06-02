package com.example.retry;

import com.example.retry.dto.ApiResponse;
import com.example.retry.service.ExternalApiService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Slf4j
class RetryIgnoreTest {

    @Autowired
    private ExternalApiService externalApiService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        // MockRestServiceServer 초기화
        mockServer = MockRestServiceServer.createServer(restTemplate);
        externalApiService.resetCounter();
        log.info("\n\n========================================");
        log.info("Test setup complete - counter reset");
        log.info("========================================\n");
    }

    @Test
    @DisplayName("400 Bad Request 에러 발생 시 retry 하지 않음")
    void test400BadRequestNoRetry() {
        log.info("\n\n========================================");
        log.info("TEST: 400 Bad Request - No Retry");
        log.info("Expected: HttpClientErrorException thrown immediately without retry");
        log.info("========================================\n");

        // Mock 서버가 400 응답을 반환하도록 설정
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8080/api/demo/bad-request"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"error\",\"code\":400,\"message\":\"Bad Request\"}"));

        // when
        ApiResponse response = externalApiService.callExternalApi("bad-request");

        // then
        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Call count: {}", externalApiService.getCallCount());
        log.info("- Response status: {}", response.getStatus());
        log.info("- Response message: {}", response.getMessage());
        log.info("========================================\n");

        // 400 에러는 ignoreExceptions에 포함되어 있으므로 1번만 호출되어야 함
        assertThat(externalApiService.getCallCount()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getMessage()).contains("400");

        mockServer.verify();
    }

    @Test
    @DisplayName("404 Not Found 에러 발생 시 retry 하지 않음")
    void test404NotFoundNoRetry() {
        log.info("\n\n========================================");
        log.info("TEST: 404 Not Found - No Retry");
        log.info("Expected: HttpClientErrorException thrown immediately without retry");
        log.info("========================================\n");

        // Mock 서버가 404 응답을 반환하도록 설정
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8080/api/demo/not-found"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"error\",\"code\":404,\"message\":\"Not Found\"}"));

        // when
        ApiResponse response = externalApiService.callExternalApi("not-found");

        // then
        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Call count: {}", externalApiService.getCallCount());
        log.info("- Response status: {}", response.getStatus());
        log.info("- Response message: {}", response.getMessage());
        log.info("========================================\n");

        // 404 에러는 ignoreExceptions에 포함되어 있으므로 1번만 호출되어야 함
        assertThat(externalApiService.getCallCount()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getMessage()).contains("404");

        mockServer.verify();
    }

    @Test
    @DisplayName("500 Internal Server Error 발생 시 retry 수행")
    void test500ServerErrorRetry() {
        log.info("\n\n========================================");
        log.info("TEST: 500 Server Error - Retry");
        log.info("Expected: Retry performed 3 times (maxAttempts: 3)");
        log.info("========================================\n");

        // Mock 서버가 500 응답을 반환하도록 설정 (3번 retry 예상)
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8080/api/demo/server-error"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"error\",\"code\":500,\"message\":\"Internal Server Error\"}"));

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8080/api/demo/server-error"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"error\",\"code\":500,\"message\":\"Internal Server Error\"}"));

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8080/api/demo/server-error"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"error\",\"code\":500,\"message\":\"Internal Server Error\"}"));

        // when
        ApiResponse response = externalApiService.callExternalApi("server-error");

        // then
        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Call count: {}", externalApiService.getCallCount());
        log.info("- Response status: {}", response.getStatus());
        log.info("- Response message: {}", response.getMessage());
        log.info("========================================\n");

        // 500 에러는 retryExceptions에 포함되어 있으므로 3번 호출되어야 함 (maxAttempts: 3)
        assertThat(externalApiService.getCallCount()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getMessage()).contains("500");

        mockServer.verify();
    }

    @Test
    @DisplayName("정상 응답 시 retry 하지 않고 성공")
    void testSuccessNoRetry() {
        log.info("\n\n========================================");
        log.info("TEST: Success Response - No Retry");
        log.info("Expected: Single call, no retry needed");
        log.info("========================================\n");

        // Mock 서버가 200 응답을 반환하도록 설정
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8080/api/demo/success"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"success\",\"message\":\"OK\"}"));

        // when
        ApiResponse response = externalApiService.callExternalApi("success");

        // then
        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Call count: {}", externalApiService.getCallCount());
        log.info("- Response status: {}", response.getStatus());
        log.info("- Response message: {}", response.getMessage());
        log.info("========================================\n");

        // 성공 시도는 1번만 호출되어야 함
        assertThat(externalApiService.getCallCount()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("OK");

        mockServer.verify();
    }

    @Test
    @DisplayName("200 성공 후 404 실패를 호출하는 시나리오")
    void testMixedScenario() {
        log.info("\n\n========================================");
        log.info("TEST: Mixed Scenario - Success then 404");
        log.info("Expected: First call succeeds (count=1), then 404 fails immediately (count=2)");
        log.info("========================================\n");

        // 첫 번째 호출: 성공
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8080/api/demo/success"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"success\",\"message\":\"OK\"}"));

        // 두 번째 호출: 404 (retry 안함)
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8080/api/demo/not-found"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"error\",\"code\":404,\"message\":\"Not Found\"}"));

        // when
        ApiResponse successResponse = externalApiService.callExternalApi("success");
        ApiResponse errorResponse = externalApiService.callExternalApi("not-found");

        // then
        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Total call count: {}", externalApiService.getCallCount());
        log.info("- Success response: {}", successResponse.getStatus());
        log.info("- Error response: {}", errorResponse.getStatus());
        log.info("========================================\n");

        assertThat(externalApiService.getCallCount()).isEqualTo(2);
        assertThat(successResponse.getStatus()).isEqualTo("success");
        assertThat(errorResponse.getStatus()).isEqualTo("error");

        mockServer.verify();
    }
}
