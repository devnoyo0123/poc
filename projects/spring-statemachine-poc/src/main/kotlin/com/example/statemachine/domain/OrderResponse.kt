package com.example.statemachine.domain

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문 응답 DTO
 *
 * Controller에서 클라이언트에게 반환할 때 사용.
 * Entity를 직접 노출하지 않고 DTO로 변환하여 API 계약을 분리.
 */
data class OrderResponse(
    val id: Long,
    val state: OrderState,
    val customerName: String,
    val productName: String,
    val amount: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Order Entity → OrderResponse DTO 변환 확장 함수
 */
fun Order.toResponse() = OrderResponse(
    id = this.id ?: throw IllegalStateException("저장되지 않은 주문입니다"),
    state = this.state,
    customerName = this.customerName,
    productName = this.productName,
    amount = this.amount,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
