package com.example.webfluxsselab.config

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DefaultDSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JooqConfig {
    @Bean
    fun dslContext(): DSLContext {
        return DefaultDSLContext(SQLDialect.POSTGRES)
    }
}
