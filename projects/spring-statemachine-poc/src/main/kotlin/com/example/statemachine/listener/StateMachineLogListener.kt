package com.example.statemachine.listener

import com.example.statemachine.domain.OrderEvent
import com.example.statemachine.domain.OrderState
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.statemachine.listener.StateMachineListenerAdapter
import org.springframework.statemachine.state.State
import org.springframework.statemachine.transition.Transition
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

/**
 * 상태 머신 로깅 리스너
 *
 * StateMachineListener: 상태 머신의 모든 라이프사이클 이벤트를 관찰하는 콜백 인터페이스.
 *
 * ⚠️ SSM 4.0 버그: stateChanged 콜백이 호출되지 않음.
 * 따라서 transition 콜백에서 target 상태를 캡처하여 OrderService에서 사용.
 */
@Component
class StateMachineLogListener : StateMachineListenerAdapter<OrderState, OrderEvent>() {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * SSM 4.0 reactive state capture
     *
     * sendEvent(Mono).block() 이후 sm.state.id를 읽으면 이전 상태가 반환됨.
     * transition 콜백에서 캡처한 target 상태를 저장.
     * 매 이벤트 전송 전에 OrderService에서 null로 리셋.
     */
    val latestNewState = AtomicReference<OrderState>(null)

    override fun stateChanged(from: State<OrderState, OrderEvent>?, to: State<OrderState, OrderEvent>?) {
        val toState = to?.id
        log.info("[LISTENER] STATE CHANGED: {} → {}", from?.id, toState)
        if (toState != null) {
            latestNewState.set(toState)
        }
    }

    /**
     * SSM 4.0 stateChanged 미동작 대응: transition 콜백에서 target 상태 캡처
     */
    override fun transition(transition: Transition<OrderState, OrderEvent>) {
        val targetState = transition.target?.id
        log.info("[LISTENER] TRANSITION: {} → {}", transition.source?.id, targetState)
        if (targetState != null) {
            latestNewState.set(targetState)
        }
    }

    override fun eventNotAccepted(event: Message<OrderEvent>?) {
        log.warn("[LISTENER] EVENT NOT ACCEPTED: {} (전환이 거부되었거나 불가능합니다)", event?.payload)
    }

    override fun transitionStarted(transition: Transition<OrderState, OrderEvent>) {
        log.debug("[LISTENER] TRANSITION STARTED: {} → {}",
            transition.source?.id, transition.target?.id)
    }

    override fun transitionEnded(transition: Transition<OrderState, OrderEvent>) {
        log.debug("[LISTENER] TRANSITION ENDED: {} → {}",
            transition.source?.id, transition.target?.id)
    }
}
