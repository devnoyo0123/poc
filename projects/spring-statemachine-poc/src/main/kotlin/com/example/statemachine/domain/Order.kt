package com.example.statemachine.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문 JPA 엔티티
 *
 * 상태 머신의 상태를 데이터베이스에 영속화하는 핵심 엔티티.
 * StateMachine의 상태가 변경될 때마다 Interceptor를 통해 이 엔티티의 state 필드가 업데이트됨.
 *
 * 핵심 개념: StateMachine은 메모리 상태이고, Order는 DB 상태.
 * 두 상태를 동기화하는 것이 Interceptor의 역할.
 */
@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 현재 주문 상태 (StateMachine과 동기화됨)
    @Enumerated(EnumType.STRING)
    var state: OrderState = OrderState.ORDER_CREATED,

    // 고객명
    val customerName: String,

    // 상품명
    val productName: String,

    // 주문 금액
    val amount: BigDecimal,

    // 생성 시각 (자동 설정)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // 수정 시각 (업데이트 시 자동 갱신)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * JPA 생명주기 콜백: 엔티티 업데이트 직전에 호출됨.
     * updatedAt 필드를 현재 시각으로 자동 갱신.
     */
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
