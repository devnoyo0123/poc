package com.example.avro.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
@RefreshScope
class FeatureFlagService {

    @Value("\${app.feature-flags.new-ui:false}")
    var newUiEnabled: Boolean = false

    @Value("\${app.feature-flags.experimental-api:false}")
    var experimentalApiEnabled: Boolean = false

    fun isNewUiEnabled(): Boolean {
        logger.info("Checking new-ui feature flag: $newUiEnabled (from @Value)")
        return newUiEnabled
    }

    fun isExperimentalApiEnabled(): Boolean {
        logger.info("Checking experimental-api feature flag: $experimentalApiEnabled (from @Value)")
        return experimentalApiEnabled
    }

    fun getAllFlags(): Map<String, Boolean> {
        return mapOf(
            "new-ui" to newUiEnabled,
            "experimental-api" to experimentalApiEnabled
        )
    }
}
