package com.example.statemachine.domain

/**
 * 주문 이벤트 enum
 *
 * Spring Statemachine에서 이벤트(Event)는 상태 전환을 유발하는 트리거.
 * 이벤트가 발생하면 설정된 전환(Transition) 규칙에 따라 상태가 변경됨.
 */
enum class OrderEvent {
    // 주문 제출 → 결제 대기로 전환
    SUBMIT_ORDER,

    // 결제 시도 (Guard에 의해 성공/실패 분기)
    ATTEMPT_PAYMENT,

    // 결제 성공
    PAYMENT_SUCCESS,

    // 결제 실패
    PAYMENT_FAIL,

    // 결제 재시도 (PAYMENT_FAILED → PAYMENT_PENDING)
    RETRY_PAYMENT,

    // 배송 준비
    PREPARE_SHIPPING,

    // 배송 시작
    START_SHIPPING,

    // 배송 지연 발생
    DELAY_DELIVERY,

    // 지연 해소 → 배송 재개
    RESUME_DELIVERY,

    // 배송 완료
    COMPLETE_DELIVERY,

    // 주문 확정 (최종 완료)
    CONFIRM_ORDER,

    // 반품 요청
    REQUEST_RETURN,

    // 반품 승인
    APPROVE_RETURN,

    // 반품 거절
    REJECT_RETURN,

    // 반품 수거
    PICK_UP_RETURN,

    // 반품 검수 완료
    INSPECT_RETURN,

    // 환불 처리
    PROCESS_REFUND,

    // 주문 취소
    CANCEL_ORDER
}
