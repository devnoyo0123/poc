package com.example.statemachine.domain

/**
 * 주문 상태 enum
 *
 * Spring Statemachine에서 상태(State)는 유한 상태 머신의 각 노드를 나타냄.
 * 각 상태는 주문의 현재 lifecycle 위치를 의미.
 */
enum class OrderState {
    // 주문 생성됨 (초기 상태 - Initial State)
    ORDER_CREATED,

    // 결제 대기중
    PAYMENT_PENDING,

    // 결제 완료
    PAYMENT_COMPLETED,

    // 결제 실패
    PAYMENT_FAILED,

    // 배송 준비중
    SHIPPING_PREPARING,

    // 배송중
    SHIPPING,

    // 배송 지연
    DELIVERY_DELAYED,

    // 배송 완료
    DELIVERED,

    // 반품 요청됨
    RETURN_REQUESTED,

    // 반품 거절됨
    RETURN_REJECTED,

    // 반품 수거 완료
    RETURN_PICKED_UP,

    // 반품 검수중
    RETURN_INSPECTING,

    // 환불 대기
    REFUND_PENDING,

    // 환불 완료 (종료 상태 - End State)
    REFUNDED,

    // 주문 완료 (종료 상태 - End State)
    COMPLETED,

    // 주문 취소 (종료 상태 - End State)
    CANCELLED
}
