package com.example.sse.webflux.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.server.HttpServer
import java.util.concurrent.TimeUnit

@Configuration
class NettyConfig {

    @Bean
    fun nettyCustomizer(): WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            val customizer = NettyServerCustomizer()
            factory.addServerCustomizers(customizer)
        }
    }

    private class NettyServerCustomizer : org.springframework.boot.web.embedded.netty.NettyServerCustomizer {
        override fun apply(server: HttpServer): HttpServer {
            return server.option(ChannelOption.SO_BACKLOG, 10000)
                .option(ChannelOption.SO_REUSEADDR, true)
                .doOnConnection { conn ->
                    conn.addHandlerLast(ReadTimeoutHandler(300, TimeUnit.SECONDS))
                    conn.addHandlerLast(WriteTimeoutHandler(300, TimeUnit.SECONDS))
                }
        }
    }
}
