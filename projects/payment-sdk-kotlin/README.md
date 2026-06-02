# Payment SDK Kotlin

**실제 SDK 구조를 배우기 위한 예제 프로젝트**

이 프로젝트는 결제 서비스를 위한 SDK를 어떻게 설계하고 구현하는지 보여줍니다.

---

## 📋 프로젝트 구조

```
lib/src/main/kotlin/com/example/payment/
├── client/
│   └── PaymentClient.kt          # 메인 클라이언트 (사용자 인터페이스)
├── config/
│   └── PaymentConfig.kt          # 설정 클래스
├── models/
│   └── PaymentModels.kt          # 요청/응답 데이터 모델
├── exceptions/
│   └── PaymentExceptions.kt      # 예외 계층 구조
├── retry/
│   └── RetryPolicy.kt            # 재시도 로직 (지수 백오프)
└── example/
    └── PaymentSDKUsage.kt        # 사용 예시
```

---

## 🎯 SDK가 제공하는 것

### 1. 복잡한 HTTP 클라이언트 숨기기

**SDK 없이:**
```java
// 개발자가 직접 HTTP 요청 빌드
RestTemplate template = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Bearer " + token);
// ... 반복적인 코드
```

**SDK 사용:**
```kotlin
val client = PaymentClient(config)
val result = client.charge(ChargeRequest(amount = 10000, orderId = "ORD-001"))
```

### 2. 자동 재시도 (Retry with Exponential Backoff)

```kotlin
// 내부적으로 자동 재시도
// 1초, 2초, 4초 지연 후 재시도
val response = client.charge(request)
```

### 3. 타입 안전한 API

```kotlin
// 컴파일 타임에 타입 체크
val result: ChargeResponse = client.charge(request)
val transactionId: String = result.transactionId
```

### 4. 일관된 에러 핸들링

```kotlin
try {
    client.charge(request)
} catch (e: AuthenticationException) {
    // API 키 문제
} catch (e: NetworkException) {
    // 네트워크 문제
} catch (e: BadRequestException) {
    // 요청 파라미터 문제
}
```

### 5. 인증 자동 처리

```kotlin
// API 키 헤더 자동 추가
val client = PaymentClient(config)
// 모든 요청에 "Authorization: Bearer {apiKey}" 자동 포함
```

---

## 🚀 빠른 시작

### 1. 의존성 추가 (build.gradle.kts)

```kotlin
dependencies {
    implementation("com.example:payment-sdk:1.0.0")
}
```

### 2. 클라이언트 초기화

```kotlin
// 방법 A: 직접 설정
val config = PaymentConfig(
    apiKey = "your-api-key",
    baseUrl = "https://api.payment.internal"
)
val client = PaymentClient(config)

// 방법 B: 환경 변수 (권장)
val config = PaymentConfig.fromEnv()
val client = PaymentClient(config)
```

### 3. 결제 요청

```kotlin
val request = ChargeRequest(
    amount = 10000L,
    currency = "KRW",
    orderId = "ORD-2024-001",
    customerId = "CUST-001",
    description = "테스트 주문"
)

val response = client.charge(request)
println("Transaction ID: ${response.transactionId}")
```

### 4. 에러 핸들링

```kotlin
try {
    val response = client.charge(request)
} catch (e: AuthenticationException) {
    println("API 키 확인 필요")
} catch (e: NetworkException) {
    println("네트워크 연결 확인 필요")
} catch (e: PaymentException) {
    println("결제 실패: ${e.message}")
}
```

---

## 📦 SDK 핵심 컴포넌트

### 1. PaymentConfig (설정)

```kotlin
data class PaymentConfig(
    val apiKey: String,           // 필수: API 키
    val baseUrl: String,         // 기본값: 결제 서비스 URL
    val timeoutSeconds: Long,    // 기본값: 30초
    val maxRetries: Int,         // 기본값: 3회
    val enableLogging: Boolean   // 기본값: true
)
```

### 2. PaymentClient (메인 클라이언트)

```kotlin
class PaymentClient(config: PaymentConfig) {
    suspend fun charge(request: ChargeRequest): ChargeResponse
    suspend fun refund(request: RefundRequest): RefundResponse
    suspend fun getTransaction(transactionId: String): Transaction
    fun close()
}
```

### 3. RetryPolicy (재시도)

- 지수 백오프 (1s → 2s → 4s → 8s)
- 네트워크 예외만 재시도
- 최대 재시도 횟수 초과 시 `RetryExhaustedException`

### 4. 예외 계층 구조

```
PaymentException (기본)
├── AuthenticationException (인증 실패)
├── NetworkException (네트워크 오류)
├── TimeoutException (타임아웃)
├── RetryExhaustedException (재시도 실패)
└── ApiException (API 응답 오류)
    └── BadRequestException (잘못된 요청)
```

---

## 🔧 빌드 및 실행

### Gradle로 빌드

```bash
cd /Users/colosseum_nohys/Documents/my/poc/projects/payment-sdk-kotlin
./gradlew build
```

### 예제 실행

```bash
./gradlew run
```

---

## 📚 실무에서 SDK 개발 시 고려사항

### 1. 버전 호환성

- **SemVer 준수**: 1.0.0 → 1.1.0 (신규 기능), 2.0.0 (breaking change)
- **deprecated 마킹**: 이전 메서드 유지

### 2. 멀티 언어 지원

```
조직 내 기술 스택:
- Java 팀 → Java SDK
- Kotlin 팀 → Kotlin SDK
- Python 팀 → Python SDK
```

### 3. 테스트 친화적

```kotlin
// 모의 객체로 쉽게 테스트
val mockClient = mockk<PaymentClient>()
coEvery { mockClient.charge(any()) } returns ChargeResponse(...)

val service = OrderService(mockClient)
service.createOrder(request)
```

### 4. 문서화

- API 문서 (Javadoc/Dokka)
- 사용 예시 (GitHub Wiki)
- Quick Start 가이드

---

## 🎓 학습 포인트

| 컴포넌트 | 역할 |
|---------|------|
| **Config** | 설정 표준화, 환경 변수 지원 |
| **Models** | 요청/응답 타입 정의 |
| **Exceptions** | 일관된 에러 처리 |
| **RetryPolicy** | 지수 백오프, 장애 복구 |
| **Client** | 사용자 인터페이스, HTTP 클라이언트 wrapper |

---

## 📖 관련 문서

- [Kafka 거버넌스 가이드](../../cs/kafka-governance.md)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [OkHttp](https://square.github.io/okhttp/)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

---

## 📝 라이선스

이 프로젝트는 학습 목적으로 만들어졌습니다.
