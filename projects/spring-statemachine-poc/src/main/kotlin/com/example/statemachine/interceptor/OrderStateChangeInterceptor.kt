package com.example.statemachine.interceptor

import com.example.statemachine.domain.OrderEvent
import com.example.statemachine.domain.OrderRepository
import com.example.statemachine.domain.OrderState
import org.slf4j.LoggerFactory
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.state.State
import org.springframework.statemachine.support.StateMachineInterceptorAdapter
import org.springframework.statemachine.transition.Transition
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

/**
 * 주문 상태 변경 인터셉터
 *
 * 핵심 개념: StateMachineInterceptor
 *
 * StateMachine의 상태 전환 라이프사이클에 끼어들어(hook) 부가 작업을 수행.
 * preStateChange는 상태 전환이 완료되기 "직전"에 호출됨.
 *
 * 이 클래스의 핵심 역할:
 * 메모리 상의 StateMachine 상태 → DB의 Order 엔티티 상태 동기화
 *
 * 패턴 설명:
 * 1. StateMachine이 전환을 실행하려 함
 * 2. preStateChange가 호출됨 (아직 DB에 반영 안 됨)
 * 3. 이 인터셉터가 DB에서 Order를 조회하고 상태를 업데이트
 * 4. 저장 완료 후 전환이 완료됨
 *
 * 이 패턴 없이는 Service에서 수동으로 DB 업데이트를 해야 하지만,
 * 인터셉터를 사용하면 자동으로 모든 전환에 대해 DB 동기화가 보장됨.
 */
@Component
class OrderStateChangeInterceptor(
    private val orderRepository: OrderRepository
) : StateMachineInterceptorAdapter<OrderState, OrderEvent>() {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 상태 전환 직전에 호출되는 콜백
     *
     * @param state       전환될 새로운 상태
     * @param message     이벤트 메시지 (헤더에 orderId가 포함되어 있음)
     * @param transition  실행 중인 전환 정보
     * @param stateMachine 현재 상태 머신
     * @param rootStateMachine 루트 상태 머신 (중첩 상태 머신인 경우)
     */
    override fun preStateChange(
        state: State<OrderState, OrderEvent>,
        message: Message<OrderEvent>?,
        transition: Transition<OrderState, OrderEvent>,
        stateMachine: StateMachine<OrderState, OrderEvent>,
        rootStateMachine: StateMachine<OrderState, OrderEvent>
    ) {
        if (message == null) {
            log.warn("[INTERCEPTOR] 메시지가 null이므로 상태 동기화를 건너뜁니다")
            return
        }

        // Message 헤더에서 orderId 추출
        val orderId = message.headers["orderId", java.lang.Long::class.java]?.toLong()
        if (orderId == null) {
            log.warn("[INTERCEPTOR] orderId 헤더가 없어 상태 동기화를 건너뜁니다")
            return
        }

        // DB에서 주문 조회 후 상태 업데이트
        val order = orderRepository.findById(orderId).orElse(null)
        if (order == null) {
            log.warn("[INTERCEPTOR] orderId={}에 해당하는 주문을 찾을 수 없습니다", orderId)
            return
        }

        val previousState = order.state
        order.state = state.id
        orderRepository.save(order)

        log.info(
            "[INTERCEPTOR] DB 상태 동기화 완료: orderId={}, {} → {}",
            orderId, previousState, state.id
        )
    }
}
