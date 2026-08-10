package com.example.statemachine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Spring Boot 메인 애플리케이션 클래스
// @SpringBootApplication은 내부적으로 @Configuration, @EnableAutoConfiguration, @ComponentScan을 포함
@SpringBootApplication
class SpringStatemachinePocApplication

fun main(args: Array<String>) {
    runApplication<SpringStatemachinePocApplication>(*args)
}
