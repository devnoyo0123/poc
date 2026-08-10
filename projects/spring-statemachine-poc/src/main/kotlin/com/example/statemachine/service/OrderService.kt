package com.example.statemachine.service

import com.example.statemachine.domain.Order
import com.example.statemachine.domain.OrderEvent
import com.example.statemachine.domain.OrderRepository
import com.example.statemachine.domain.OrderState
import com.example.statemachine.interceptor.OrderStateChangeInterceptor
import com.example.statemachine.listener.StateMachineLogListener
import org.slf4j.LoggerFactory
import org.springframework.messaging.support.MessageBuilder
import org.springframework.statemachine.StateMachine
import org.springframework.statemachine.config.StateMachineFactory
import org.springframework.statemachine.listener.StateMachineListenerAdapter
import org.springframework.statemachine.state.State
import org.springframework.statemachine.support.DefaultStateMachineContext
import org.springframework.statemachine.transition.Transition
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

/**
 * 주문 서비스
 *
 * 핵심 패턴: build → sendEvent
 *
 * 실무에서 Spring Statemachine을 사용할 때 가장 중요한 패턴.
 * 매 이벤트 전송 시마다:
 *   1. Factory에서 새 StateMachine을 생성 (build)
 *   2. DB에 저장된 현재 상태로 StateMachine을 초기화 (reset)
 *   3. 인터셉터를 등록하여 상태 변경 시 DB 동기화 보장
 *   4. 이벤트를 전송하여 상태 전환 유발 (sendEvent)
 *
 * 이 패턴이 필요한 이유:
 * - StateMachine은 메모리 객체 → 서버 재시작 시 상태가 사라짐
 * - 따라서 매 요청마다 DB 상태를 기반으로 StateMachine을 재구성해야 함
 * - @EnableStateMachineFactory를 사용하는 이유가 바로 이것
 */
@Service
class OrderService(
    private val stateMachineFactory: StateMachineFactory<OrderState, OrderEvent>,
    private val orderRepository: OrderRepository,
    private val orderStateChangeInterceptor: OrderStateChangeInterceptor,
    private val stateMachineLogListener: StateMachineLogListener
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // Message 헤더 키 상수
        const val ORDER_ID_HEADER = "orderId"
        const val PAYMENT_VALID_HEADER = "paymentValid"
        const val CUSTOMER_NAME_HEADER = "customerName"
        const val PRODUCT_NAME_HEADER = "productName"
    }

    /**
     * 새 주문 생성
     *
     * 주문을 DB에 저장하고 초기 상태(ORDER_CREATED)로 시작.
     * StateMachine은 아직 생성하지 않음 (이벤트 전송 시 생성됨).
     */
    fun createOrder(customerName: String, productName: String, amount: BigDecimal): Order {
        val order = Order(
            customerName = customerName,
            productName = productName,
            amount = amount
        )
        val saved = orderRepository.save(order)
        log.info("[SERVICE] 주문 생성됨: orderId={}, customer={}, product={}, amount={}",
            saved.id, customerName, productName, amount)
        return saved
    }

    /**
     * 상태 머신 이벤트 전송
     *
     * build → sendEvent 패턴의 핵심 메서드.
     *
     * ⚠️ SSM 4.0의 critical issue:
     *   sendEvent(Mono)의 reactive pipeline이 완료되어도,
     *   StateMachine 내부의 state 필드가 다른 스레드(reactor scheduler)에서
     *   비동기적으로 업데이트됨. 따라서 sm.state.id를 읽으면 이전 상태가 반환됨.
     *
     * 해결책: StateMachineLogListener의 stateChanged 캡처 + polling fallback
     *   - StateMachineLogListener는 build()에서 start() 이전에 등록되므로
     *     stateChanged 콜백이 정상적으로 발생함
     *   - latestNewState AtomicReference에서 캡처된 상태를 읽음
     *   - 콜백이 아직 도달하지 않은 경우 sm.state.id를 polling하여 최종 동기화
     *
     * 1. DB에서 주문 조회
     * 2. StateMachine 빌드 (현재 DB 상태로 초기화)
     * 3. latestNewState 리셋
     * 4. 이벤트 메시지 생성 및 전송
     * 5. Listener에서 캡처한 상태 조회, 필요시 polling fallback
     * 6. DB 업데이트
     */
    fun sendEvent(orderId: Long, event: OrderEvent, headers: Map<String, Any> = emptyMap()): OrderState {
        val order = orderRepository.findById(orderId).orElseThrow {
            IllegalArgumentException("주문을 찾을 수 없습니다: orderId=$orderId")
        }

        log.info("[SERVICE] 이벤트 전송: orderId={}, currentState={}, event={}",
            orderId, order.state, event)

        val newStateRef = AtomicReference<OrderState>(null)

        val sm = build(orderId, order, newStateRef)

        val messageBuilder = MessageBuilder
            .withPayload(event)
            .setHeader(ORDER_ID_HEADER, orderId)
            .setHeader(CUSTOMER_NAME_HEADER, order.customerName)
            .setHeader(PRODUCT_NAME_HEADER, order.productName)

        headers.forEach { (key, value) ->
            messageBuilder.setHeader(key, value)
        }

        val message = messageBuilder.build()

        val results = sm.sendEvent(Mono.just(message))
            .doOnNext { r ->
                log.info("[SERVICE] reactive result: resultType={}", r.resultType)
            }
            .collectList()
            .block()

        val accepted = results?.any {
            it.resultType == org.springframework.statemachine.StateMachineEventResult.ResultType.ACCEPTED
        } == true
        log.info("[SERVICE] 이벤트 전송 결과: accepted={}", accepted)

        Thread.sleep(50)

        var newState: OrderState? = newStateRef.get()

        if (newState == null || newState == order.state) {
            val deadline = System.currentTimeMillis() + 2000
            while (System.currentTimeMillis() < deadline) {
                val smState = sm.state.id
                if (smState != order.state) {
                    newState = smState
                    break
                }
                Thread.sleep(10)
            }
        }

        if (newState == null) {
            newState = order.state
            log.warn("[SERVICE] 상태 전환 감지 실패: orderId={}. 이전 상태 유지.", orderId)
        }

        if (newState != order.state) {
            order.state = newState
            orderRepository.save(order)
        }

        log.info("[SERVICE] 상태 전환 완료: orderId={}, newState={}, sm.state.id={}",
            orderId, newState, sm.state.id)
        return newState
    }

    /**
     * 주문 ID로 조회
     */
    fun getOrder(orderId: Long): Order =
        orderRepository.findById(orderId).orElseThrow {
            IllegalArgumentException("주문을 찾을 수 없습니다: orderId=$orderId")
        }

    /**
     * 전체 주문 조회
     */
    fun getAllOrders(): List<Order> = orderRepository.findAll()

    /**
     * StateMachine 빌드 (프라이빗 메서드)
     *
     * 핵심 빌드 패턴:
     * 1. Factory에서 StateMachine 생성 (orderId를 machineId로 사용 → 고유 식별)
     * 2. StateMachine 정지 (상태 리셋을 위해)
     * 3. StateMachineAccessor로 내부 상태에 접근:
     *    a. 인터셉터 등록 (상태 변경 시 DB 동기화)
     *    b. DefaultStateMachineContext로 현재 DB 상태로 초기화
     * 4. StateMachine 시작
     *
     * @param orderId 주문 ID (StateMachine의 식별자로 사용)
     * @param order   현재 주문 (초기 상태로 사용)
     */
    private fun build(orderId: Long, order: Order, newStateRef: AtomicReference<OrderState>): StateMachine<OrderState, OrderEvent> {
        val sm = stateMachineFactory.getStateMachine(orderId.toString())

        // SSM 4.0: reactive lifecycle API 사용
        sm.stopReactively().block()
        sm.stateMachineAccessor.doWithAllRegions { accessor ->
            accessor.addStateMachineInterceptor(orderStateChangeInterceptor)
            accessor.resetStateMachine(
                DefaultStateMachineContext(order.state, null, null, null)
            )
        }

        sm.addStateListener(stateMachineLogListener)

        // SSM 4.0 버그 대응: stateChanged 콜백이 호출되지 않음.
        // transition 콜백에서 target 상태를 캡처하는 익명 리스너를 매 요청마다 새로 등록.
        sm.addStateListener(object : StateMachineListenerAdapter<OrderState, OrderEvent>() {
            override fun transition(transition: Transition<OrderState, OrderEvent>) {
                val target = transition.target?.id
                if (target != null) {
                    log.info("[SERVICE][CAPTURE] transition 캡처: {} → {}", transition.source?.id, target)
                    newStateRef.set(target)
                }
            }
        })

        sm.startReactively().block()

        log.info("[SERVICE] StateMachine 빌드 완료: orderId={}, initialState={}",
            orderId, sm.state.id)

        return sm
    }
}
