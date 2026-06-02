package com.example.payment.example

import com.example.payment.client.PaymentClient
import com.example.payment.config.PaymentConfig
import com.example.payment.models.ChargeRequest
import kotlinx.coroutines.runBlocking

/**
 * Payment SDK 사용 예시
 *
 * 이 파일은 SDK 사용법을 보여주는 예제 코드입니다.
 */
fun main() = runBlocking {
    // 1. SDK 클라이언트 초기화

    // 방법 A: 직접 설정
    val config = PaymentConfig(
        apiKey = "your-api-key-here",
        baseUrl = "https://api.payment.internal",
        timeoutSeconds = 30,
        maxRetries = 3,
        enableLogging = true
    )

    // 방법 B: 환경 변수로부터 설정 (권장)
    // val config = PaymentConfig.fromEnv()

    // 방법 C: 테스트용 설정
    // val config = PaymentConfig.forTest()

    val client = PaymentClient(config)


    // 2. 결제 요청 (Charge)
    try {
        val chargeRequest = ChargeRequest(
            amount = 10000L,
            currency = "KRW",
            orderId = "ORD-2024-001",
            customerId = "CUST-001",
            description = "테스트 주문",
            metadata = mapOf(
                "source" to "mobile_app",
                "campaign" to "summer_sale"
            )
        )

        val chargeResponse = client.charge(chargeRequest)
        println("결제 성공!")
        println("- Transaction ID: ${chargeResponse.transactionId}")
        println("- Status: ${chargeResponse.status}")
        println("- Amount: ${chargeResponse.amount} ${chargeResponse.currency}")

    } catch (e: Exception) {
        println("결제 실패: ${e.message}")
        when (e) {
            is com.example.payment.exceptions.AuthenticationException -> {
                println("API 키를 확인해주세요")
            }
            is com.example.payment.exceptions.NetworkException -> {
                println("네트워크 연결을 확인해주세요")
            }
            is com.example.payment.exceptions.BadRequestException -> {
                println("요청 파라미터를 확인해주세요: ${e.field}")
            }
            else -> {
                println("알 수 없는 오류가 발생했습니다")
            }
        }
    }


    // 3. 거래 조회 (Get Transaction)
    try {
        val transaction = client.getTransaction("TX-123456")
        println("거래 정보:")
        println("- Transaction ID: ${transaction.transactionId}")
        println("- Status: ${transaction.status}")
        println("- Created At: ${transaction.createdAt}")

    } catch (e: Exception) {
        println("거래 조회 실패: ${e.message}")
    }


    // 4. 환불 요청 (Refund)
    try {
        val refundRequest = com.example.payment.models.RefundRequest(
            transactionId = "TX-123456",
            amount = 5000L,  // 부분 환불
            reason = "고객 요청"
        )

        val refundResponse = client.refund(refundRequest)
        println("환불 성공!")
        println("- Refund ID: ${refundResponse.refundId}")
        println("- Amount: ${refundResponse.amount}")

    } catch (e: Exception) {
        println("환불 실패: ${e.message}")
    }


    // 5. 리소스 정리
    client.close()
}


/**
 * Spring Boot에서의 사용 예시
 */
/*
@Service
class OrderService {

    private val paymentClient = PaymentClient(
        PaymentConfig.fromEnv()
    )

    fun createOrder(request: CreateOrderRequest): Order {
        // 1. 결제 시도
        val chargeResponse = try {
            paymentClient.charge(
                ChargeRequest(
                    amount = request.amount,
                    orderId = request.orderId,
                    customerId = request.customerId,
                    description = "주문 결제"
                )
            )
        } catch (e: com.example.payment.exceptions.PaymentException) {
            throw OrderCreationException("결제 실패: ${e.message}", e)
        }

        // 2. 주문 저장
        val order = Order(
            id = request.orderId,
            transactionId = chargeResponse.transactionId,
            amount = chargeResponse.amount,
            status = OrderStatus.PAID
        )
        orderRepository.save(order)

        return order
    }
}
*/
