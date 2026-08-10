package com.example.statemachine.domain

import org.springframework.data.jpa.repository.JpaRepository

/**
 * 주문 리포지토리
 *
 * Spring Data JPA가 자동으로 CRUD 구현체를 생성.
 * StateMachine Interceptor와 Service에서 주문 엔티티를 조회/저장할 때 사용됨.
 */
interface OrderRepository : JpaRepository<Order, Long>
