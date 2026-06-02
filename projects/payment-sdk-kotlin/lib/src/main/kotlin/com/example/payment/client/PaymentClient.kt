package com.example.payment.client

import com.example.payment.config.PaymentConfig
import com.example.payment.exceptions.*
import com.example.payment.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 결제 서비스 API 클라이언트
 *
 * 이 클래스는 SDK 사용자가 직접 사용하는 메인 인터페이스입니다.
 */
class PaymentClient(config: PaymentConfig) {
    private val httpClient: OkHttpClient
    private val baseUrl: String
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val retryPolicy = com.example.payment.retry.RetryPolicy(
        maxAttempts = config.maxRetries
    )

    private val JSON_MEDIA_TYPE = "application/json".toMediaType()

    init {
        this.baseUrl = config.baseUrl

        val builder = OkHttpClient.Builder()
            .connectTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("Authorization", "Bearer ${config.apiKey}")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PaymentSDK-Kotlin/1.0.0")

                if (config.enableLogging) {
                    println("[PaymentSDK] ${originalRequest.method} ${originalRequest.url}")
                }

                chain.proceed(requestBuilder.build())
            }

        this.httpClient = builder.build()
    }

    /**
     * 결제 요청
     *
     * @param request 결제 요청 정보
     * @return 결제 응답
     * @throws ApiException API 호출 실패
     * @throws AuthenticationException 인증 실패
     * @throws NetworkException 네트워크 오류
     */
    suspend fun charge(request: ChargeRequest): ChargeResponse {
        return retryPolicy.execute {
            postAndParse("/api/v1/charges", request)
        }
    }

    /**
     * 환불 요청
     *
     * @param request 환불 요청 정보
     * @return 환불 응답
     */
    suspend fun refund(request: RefundRequest): RefundResponse {
        return retryPolicy.execute {
            postAndParse("/api/v1/refunds", request)
        }
    }

    /**
     * 거래 조회
     *
     * @param transactionId 거래 ID
     * @return 거래 정보
     */
    suspend fun getTransaction(transactionId: String): Transaction {
        return retryPolicy.execute {
            getAndParse("/api/v1/transactions/$transactionId")
        }
    }

    /**
     * POST 요청 및 역직렬화 (내부)
     */
    private suspend inline fun <reified R> postAndParse(
        path: String,
        body: Any
    ): R = withContext(Dispatchers.IO) {
        val jsonBody = json.encodeToString(body)
        val requestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(requestBody)
            .build()

        val responseBody = executeRequest(request)
        json.decodeFromString(responseBody)
    }

    /**
     * GET 요청 및 역직렬화 (내부)
     */
    private suspend inline fun <reified R> getAndParse(path: String): R = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .get()
            .build()

        val responseBody = executeRequest(request)
        json.decodeFromString(responseBody)
    }

    /**
     * HTTP 요청 실행 (내부)
     */
    private suspend fun executeRequest(request: Request): String = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.newCall(request).execute()

            when (response.code) {
                401 -> throw AuthenticationException("API 키가 유효하지 않습니다")
                404 -> throw ApiException(404, null, "리소스를 찾을 수 없습니다: ${request.url}")
                in 400..499 -> {
                    val body = response.body?.string() ?: ""
                    throw BadRequestException(message = "클라이언트 오류: HTTP ${response.code} - $body")
                }
                in 500..599 -> {
                    val body = response.body?.string() ?: ""
                    throw ApiException(response.code, null, "서버 오류: HTTP ${response.code} - $body")
                }
            }

            val responseBody = response.body?.string()
                ?: throw ApiException(response.code, null, "응답 본문이 비어있습니다")

            responseBody

        } catch (e: IOException) {
            throw NetworkException("네트워크 오류: ${e.message}", e)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw TimeoutException("요청 타임아웃")
        }
    }

    /**
     * 리소스 정리
     */
    fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }
}
