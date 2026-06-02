package com.example.avro.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "schema.registry")
data class SerializerConfig(
    var type: String? = null,
    var url: String? = null,
    var region: String? = null,
    var registryName: String? = null
)
