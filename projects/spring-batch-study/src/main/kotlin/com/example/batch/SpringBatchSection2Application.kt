package com.example.batch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing

@SpringBootApplication
@EnableBatchProcessing
class SpringBatchSection2Application

fun main(args: Array<String>) {
    runApplication<SpringBatchSection2Application>(*args)
}
