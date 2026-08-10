package com.example.statemachine.config

import com.example.statemachine.domain.OrderEvent
import com.example.statemachine.domain.OrderState
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.action.Action
import org.springframework.statemachine.config.EnableStateMachineFactory
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer
import org.springframework.statemachine.guard.Guard

/**
 * 주문 상태 머신 설정 클래스
 *
 * 핵심 개념:
 * - @EnableStateMachineFactory: 싱글톤이 아닌 Factory를 생성.
 *   각 주문마다 독립적인 StateMachine 인스턴스를 생성할 수 있음.
 *   (@EnableStateMachine은 싱글톤 하나만 생성하므로 실무에서는 부적합)
 *
 * - EnumStateMachineConfigurerAdapter<S, E>: 상태(S)와 이벤트(E)를 enum으로
 *   사용하는 상태 머신 설정의 베이스 클래스.
 *
 * - State: 유한 상태 머신의 각 노드 (OrderState enum)
 * - Event: 상태 전환을 유발하는 트리거 (OrderEvent enum)
 * - Transition: (Source State, Event) → Target State 매핑
 * - Guard: 전환 실행 여부를 결정하는 조건 (boolean 반환)
 * - Action: 전환 실행 시 수행할 부가 작업 (로깅, 알림 등)
 */
@Configuration
@EnableStateMachineFactory
class OrderStateMachineConfig : EnumStateMachineConfigurerAdapter<OrderState, OrderEvent>() {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // Message 헤더 키: 결제 유효성 플래그
        const val PAYMENT_VALID_HEADER = "paymentValid"
    }

    /**
     * 상태(State) 설정
     *
     * - initial(): 초기 상태 설정. StateMachine이 시작될 때 이 상태가 됨.
     * - end(): 종료 상태 설정. 이 상태에 도달하면 StateMachine이 종료됨.
     * - states(): enum의 모든 값을 상태로 등록.
     */
    override fun configure(states: StateMachineStateConfigurer<OrderState, OrderEvent>) {
        states
            .withStates()
            .initial(OrderState.ORDER_CREATED)
            .end(OrderState.COMPLETED)
            .end(OrderState.CANCELLED)
            .end(OrderState.REFUNDED)
            .states(OrderState.entries.toSet())
    }

    /**
     * 전환(Transition) 설정
     *
     * 핵심 개념:
     * - source(): 전환의 출발 상태
     * - target(): 전환의 도착 상태
     * - event(): 전환을 유발하는 이벤트
     * - guard(): 전환 실행 조건 (true여야 전환 실행)
     * - action(): 전환 시 실행할 동작
     *
     * 구조: source + event → target (선택적으로 guard, action 추가)
     */
    override fun configure(transitions: StateMachineTransitionConfigurer<OrderState, OrderEvent>) {
        transitions
            // ── 주문 생성 → 결제 ──────────────────────────────────
            .withExternal()
            .source(OrderState.ORDER_CREATED).target(OrderState.PAYMENT_PENDING)
            .event(OrderEvent.SUBMIT_ORDER)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            // ── 결제 흐름 ─────────────────────────────────────────
            .and().withExternal()
            .source(OrderState.PAYMENT_PENDING).target(OrderState.PAYMENT_COMPLETED)
            .event(OrderEvent.ATTEMPT_PAYMENT)
            .guard(paymentAmountValid())
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.PAYMENT_PENDING).target(OrderState.PAYMENT_FAILED)
            .event(OrderEvent.ATTEMPT_PAYMENT)
            .guard(paymentAmountInvalid())
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.PAYMENT_FAILED).target(OrderState.PAYMENT_PENDING)
            .event(OrderEvent.RETRY_PAYMENT)
            .action(logTransitionAction())

            // ── 결제 완료 → 배송 ──────────────────────────────────
            .and().withExternal()
            .source(OrderState.PAYMENT_COMPLETED).target(OrderState.SHIPPING_PREPARING)
            .event(OrderEvent.PREPARE_SHIPPING)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            // ── 배송 흐름 ─────────────────────────────────────────
            .and().withExternal()
            .source(OrderState.SHIPPING_PREPARING).target(OrderState.SHIPPING)
            .event(OrderEvent.START_SHIPPING)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.SHIPPING).target(OrderState.DELIVERY_DELAYED)
            .event(OrderEvent.DELAY_DELIVERY)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.DELIVERY_DELAYED).target(OrderState.SHIPPING)
            .event(OrderEvent.RESUME_DELIVERY)
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.SHIPPING).target(OrderState.DELIVERED)
            .event(OrderEvent.COMPLETE_DELIVERY)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            // ── 배송 완료 → 주문 완료 ──────────────────────────────
            .and().withExternal()
            .source(OrderState.DELIVERED).target(OrderState.COMPLETED)
            .event(OrderEvent.CONFIRM_ORDER)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            // ── 반품 흐름 (배송중 or 배송완료에서 반품 요청 가능) ──
            .and().withExternal()
            .source(OrderState.SHIPPING).target(OrderState.RETURN_REQUESTED)
            .event(OrderEvent.REQUEST_RETURN)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.DELIVERED).target(OrderState.RETURN_REQUESTED)
            .event(OrderEvent.REQUEST_RETURN)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.RETURN_REQUESTED).target(OrderState.RETURN_PICKED_UP)
            .event(OrderEvent.APPROVE_RETURN)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.RETURN_REQUESTED).target(OrderState.RETURN_REJECTED)
            .event(OrderEvent.REJECT_RETURN)
            .action(notifyCustomerAction())
            .action(logTransitionAction())

            // 반품 거절 후 재요청 가능
            .and().withExternal()
            .source(OrderState.RETURN_REJECTED).target(OrderState.RETURN_REQUESTED)
            .event(OrderEvent.REQUEST_RETURN)
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.RETURN_PICKED_UP).target(OrderState.RETURN_INSPECTING)
            .event(OrderEvent.INSPECT_RETURN)
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.RETURN_INSPECTING).target(OrderState.REFUNDED)
            .event(OrderEvent.PROCESS_REFUND)
            .action(notifyCustomerAction())
            .action(restoreStockAction())
            .action(logTransitionAction())

            // ── 주문 취소 (여러 상태에서 가능) ─────────────────────
            .and().withExternal()
            .source(OrderState.PAYMENT_PENDING).target(OrderState.CANCELLED)
            .event(OrderEvent.CANCEL_ORDER)
            .action(notifyCustomerAction())
            .action(restoreStockAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.PAYMENT_FAILED).target(OrderState.CANCELLED)
            .event(OrderEvent.CANCEL_ORDER)
            .action(notifyCustomerAction())
            .action(restoreStockAction())
            .action(logTransitionAction())

            .and().withExternal()
            .source(OrderState.SHIPPING_PREPARING).target(OrderState.CANCELLED)
            .event(OrderEvent.CANCEL_ORDER)
            .action(notifyCustomerAction())
            .action(restoreStockAction())
            .action(logTransitionAction())
    }

    /**
     * Guard: 결제 금액 유효성 검사
     *
     * Message 헤더에서 "paymentValid" 값을 읽어서 true인지 확인.
     * Guard가 true를 반환하면 전환이 실행되고, false면 전환이 차단됨.
     *
     * 사용 예: MessageBuilder.withPayload(event).setHeader("paymentValid", true)
     */
    @Bean
    fun paymentAmountValid(): Guard<OrderState, OrderEvent> = Guard { context ->
        val paymentValid = context.messageHeaders[PAYMENT_VALID_HEADER, java.lang.Boolean::class.java]?.booleanValue() ?: true
        log.info("[GUARD] paymentAmountValid = {} (orderId={})", paymentValid, context.messageHeaders["orderId"])
        paymentValid
    }

    /**
     * Guard: 결제 금액 무효 (paymentAmountValid의 부정)
     *
     * 동일한 이벤트(ATTEMPT_PAYMENT)에 대해 두 개의 전환을 설정하고,
     * 각각 다른 Guard로 분기하는 패턴.
     * paymentValid=false이면 PAYMENT_FAILED로 전환됨.
     */
    @Bean
    fun paymentAmountInvalid(): Guard<OrderState, OrderEvent> = Guard { context ->
        val paymentValid = context.messageHeaders[PAYMENT_VALID_HEADER, java.lang.Boolean::class.java]?.booleanValue() ?: true
        log.info("[GUARD] paymentAmountInvalid check → paymentValid={}", paymentValid)
        !paymentValid
    }

    /**
     * Action: 고객 알림
     *
     * 상태 전환 시 고객에게 알림을 보내는 액션 (POC에서는 로깅으로 대체).
     * 실무에서는 이메일, SMS, 푸시 알림 등을 여기서 처리.
     */
    @Bean
    fun notifyCustomerAction(): Action<OrderState, OrderEvent> = Action { context ->
        val customerName = context.messageHeaders["customerName", String::class.java] ?: "알 수 없음"
        val event = context.event
        val targetState = context.target?.id
        log.info("[ACTION] 고객 {}에게 알림: {} → {}", customerName, event, targetState)
    }

    /**
     * Action: 재고 복구
     *
     * 취소/환불 시 재고를 복구하는 액션 (POC에서는 로깅으로 대체).
     * 실무에서는 재고 서비스 API 호출 등을 여기서 처리.
     */
    @Bean
    fun restoreStockAction(): Action<OrderState, OrderEvent> = Action { context ->
        val productName = context.messageHeaders["productName", String::class.java] ?: "알 수 없음"
        log.info("[ACTION] 재고 복구: {}", productName)
    }

    /**
     * Action: 상태 전환 로깅
     *
     * 모든 전환에 적용되는 공통 로깅 액션.
     * 전환의 출발 상태, 도착 상태, 이벤트를 로그로 기록.
     */
    @Bean
    fun logTransitionAction(): Action<OrderState, OrderEvent> = Action { context ->
        val sourceState = context.source?.id
        val targetState = context.target?.id
        val event = context.event
        log.info("[ACTION] 상태 전환: {} → {} (이벤트: {})", sourceState, targetState, event)
    }
}
