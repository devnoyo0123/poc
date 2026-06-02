package com.example.s3sqs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class S3SqsLocalstackPocApplication

fun main(args: Array<String>) {
    runApplication<S3SqsLocalstackPocApplication>(*args)
}
