package com.example.payment.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 결제 요청
 */
@Serializable
data class ChargeRequest(
    val amount: Long,
    val currency: String = "KRW",
    val orderId: String,
    @SerialName("customer_id")
    val customerId: String? = null,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 결제 응답
 */
@Serializable
data class ChargeResponse(
    @SerialName("transaction_id")
    val transactionId: String,
    val status: String,
    val amount: Long,
    val currency: String,
    @SerialName("created_at")
    val createdAt: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 환불 요청
 */
@Serializable
data class RefundRequest(
    @SerialName("transaction_id")
    val transactionId: String,
    val amount: Long? = null,  // null이면 전액 환불
    val reason: String? = null
)

/**
 * 환불 응답
 */
@Serializable
data class RefundResponse(
    @SerialName("refund_id")
    val refundId: String,
    @SerialName("transaction_id")
    val transactionId: String,
    val amount: Long,
    val status: String,
    @SerialName("created_at")
    val createdAt: String
)

/**
 * 거래 조회 응답
 */
@Serializable
data class Transaction(
    @SerialName("transaction_id")
    val transactionId: String,
    val status: String,
    val amount: Long,
    val currency: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)
