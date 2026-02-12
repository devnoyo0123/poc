package com.example.sse.servlet.config

import org.apache.catalina.connector.Connector
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TomcatConfig {

    @Bean
    fun tomcatCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            val customizer = TomcatConnectorCustomizer()
            factory.addConnectorCustomizers(customizer)
        }
    }

    private class TomcatConnectorCustomizer : org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer {
        override fun customize(connector: Connector) {
            // Configure Tomcat connector for C10K
            connector.setProperty("maxConnections", "10000")
            connector.setProperty("acceptCount", "1000")
            connector.setProperty("connectionTimeout", "300000") // 5 minutes
            connector.setProperty("keepAliveTimeout", "300000")
            connector.setProperty("maxKeepAliveRequests", "1000")
        }
    }
}
