package com.example.statemachine.controller

import com.example.statemachine.domain.OrderEvent
import com.example.statemachine.domain.OrderResponse
import com.example.statemachine.domain.toResponse
import com.example.statemachine.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/**
 * 주문 REST API 컨트롤러
 *
 * 각 엔드포인트는:
 * 1. OrderService.sendEvent()를 호출하여 상태 머신 이벤트 전송
 * 2. 전송 결과(새로운 상태)를 받아서 Order 엔티티를 다시 조회
 * 3. OrderResponse DTO로 변환하여 반환
 *
 * 흐름: HTTP 요청 → Controller → Service(build+sendEvent) → StateMachine → Interceptor(DB동기화) → 응답
 */
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {

    /**
     * 주문 생성
     * POST /api/orders
     */
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val order = orderService.createOrder(
            customerName = request.customerName,
            productName = request.productName,
            amount = request.amount
        )
        return ResponseEntity.ok(order.toResponse())
    }

    /**
     * 전체 주문 조회
     * GET /api/orders
     */
    @GetMapping
    fun getAllOrders(): ResponseEntity<List<OrderResponse>> {
        val orders = orderService.getAllOrders().map { it.toResponse() }
        return ResponseEntity.ok(orders)
    }

    /**
     * 주문 단건 조회
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        val order = orderService.getOrder(id)
        return ResponseEntity.ok(order.toResponse())
    }

    /**
     * 주문 제출: ORDER_CREATED → PAYMENT_PENDING
     * POST /api/orders/{id}/submit
     */
    @PostMapping("/{id}/submit")
    fun submitOrder(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.SUBMIT_ORDER)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 결제 시도: PAYMENT_PENDING → PAYMENT_COMPLETED 또는 PAYMENT_FAILED
     * POST /api/orders/{id}/pay?paymentValid=true/false
     *
     * paymentValid 파라미터로 결제 성공/실패를 시뮬레이션.
     * Guard가 이 값을 확인하여 전환 대상을 결정.
     */
    @PostMapping("/{id}/pay")
    fun attemptPayment(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "true") paymentValid: Boolean
    ): ResponseEntity<OrderResponse> {
        orderService.sendEvent(
            id, OrderEvent.ATTEMPT_PAYMENT,
            mapOf("paymentValid" to paymentValid)
        )
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 결제 재시도: PAYMENT_FAILED → PAYMENT_PENDING
     * POST /api/orders/{id}/retry-payment
     */
    @PostMapping("/{id}/retry-payment")
    fun retryPayment(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.RETRY_PAYMENT)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 배송 준비: PAYMENT_COMPLETED → SHIPPING_PREPARING
     * POST /api/orders/{id}/prepare-shipping
     */
    @PostMapping("/{id}/prepare-shipping")
    fun prepareShipping(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.PREPARE_SHIPPING)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 배송 시작: SHIPPING_PREPARING → SHIPPING
     * POST /api/orders/{id}/start-shipping
     */
    @PostMapping("/{id}/start-shipping")
    fun startShipping(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.START_SHIPPING)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 배송 지연: SHIPPING → DELIVERY_DELAYED
     * POST /api/orders/{id}/delay
     */
    @PostMapping("/{id}/delay")
    fun delayDelivery(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.DELAY_DELIVERY)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 배송 재개: DELIVERY_DELAYED → SHIPPING
     * POST /api/orders/{id}/resume
     */
    @PostMapping("/{id}/resume")
    fun resumeDelivery(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.RESUME_DELIVERY)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 배송 완료: SHIPPING → DELIVERED
     * POST /api/orders/{id}/deliver
     */
    @PostMapping("/{id}/deliver")
    fun completeDelivery(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.COMPLETE_DELIVERY)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 주문 확정: DELIVERED → COMPLETED (종료 상태)
     * POST /api/orders/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    fun confirmOrder(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.CONFIRM_ORDER)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 반품 요청: SHIPPING/DELIVERED → RETURN_REQUESTED
     * POST /api/orders/{id}/request-return
     */
    @PostMapping("/{id}/request-return")
    fun requestReturn(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.REQUEST_RETURN)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 반품 승인: RETURN_REQUESTED → RETURN_PICKED_UP
     * POST /api/orders/{id}/approve-return
     */
    @PostMapping("/{id}/approve-return")
    fun approveReturn(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.APPROVE_RETURN)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 반품 거절: RETURN_REQUESTED → RETURN_REJECTED
     * POST /api/orders/{id}/reject-return
     */
    @PostMapping("/{id}/reject-return")
    fun rejectReturn(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.REJECT_RETURN)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 반품 검수: RETURN_PICKED_UP → RETURN_INSPECTING
     * POST /api/orders/{id}/inspect-return
     */
    @PostMapping("/{id}/inspect-return")
    fun inspectReturn(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.INSPECT_RETURN)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 환불 처리: RETURN_INSPECTING → REFUNDED (종료 상태)
     * POST /api/orders/{id}/refund
     */
    @PostMapping("/{id}/refund")
    fun processRefund(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.PROCESS_REFUND)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }

    /**
     * 주문 취소: PAYMENT_PENDING/PAYMENT_FAILED/SHIPPING_PREPARING → CANCELLED
     * POST /api/orders/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    fun cancelOrder(@PathVariable id: Long): ResponseEntity<OrderResponse> {
        orderService.sendEvent(id, OrderEvent.CANCEL_ORDER)
        return ResponseEntity.ok(orderService.getOrder(id).toResponse())
    }
}

/**
 * 주문 생성 요청 DTO
 */
data class CreateOrderRequest(
    val customerName: String,
    val productName: String,
    val amount: BigDecimal
)
