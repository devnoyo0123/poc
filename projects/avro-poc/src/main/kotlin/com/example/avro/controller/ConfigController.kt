package com.example.avro.controller

import com.example.avro.config.AppConfig
import com.example.avro.model.ConfigChangeEvent
import com.example.avro.model.toGenericRecord
import com.example.avro.producer.UserEventProducer
import com.example.avro.service.FeatureFlagService
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/config")
class ConfigController(
    private val appConfig: AppConfig,
    private val eventProducer: UserEventProducer,
    private val featureFlagService: FeatureFlagService
) {

    @GetMapping
    fun getCurrentConfig(): Map<String, Any> {
        return mapOf(
            "featureFlags" to appConfig.featureFlags,
            "timeouts" to appConfig.timeouts,
            "limits" to appConfig.limits
        )
    }

    @GetMapping("/refreshed")
    fun getRefreshedConfig(): Map<String, Any> {
        // @RefreshScope Bean에서 읽은 값 (진짜 Spring Property)
        return mapOf(
            "featureFlags" to featureFlagService.getAllFlags(),
            "note" to "These values are from @Value in @RefreshScope bean",
            "source" to "Spring Environment"
        )
    }

    @PostMapping("/feature-flag")
    fun updateFeatureFlag(@RequestBody request: UpdateConfigRequest): Map<String, Any> {
        logger.info("📝 Updating feature flag: ${request.key} = ${request.value}")

        // 로컬 설정 업데이트
        appConfig.updateFeatureFlag(request.key, request.value as Boolean)

        // Kafka 이벤트 발행
        publishConfigEvent("featureFlag", request.key, request.value)

        return mapOf(
            "success" to true,
            "message" to "Feature flag updated and event published",
            "configType" to "featureFlag",
            "key" to request.key,
            "value" to request.value
        )
    }

    private fun publishConfigEvent(configType: String, key: String, value: Any) {
        val event = ConfigChangeEvent(
            configType = configType,
            key = key,
            value = value,
            timestamp = System.currentTimeMillis(),
            source = "api-server"
        )

        val record = event.toGenericRecord()
        eventProducer.sendGenericRecord("config-events", key, record)

        logger.info("📤 Config event published: $configType.$key = $value")
    }

}

data class UpdateConfigRequest(
    val key: String,
    val value: Any
)
