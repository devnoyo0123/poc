package com.example.avro.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app")
data class AppConfig(
    var featureFlags: MutableMap<String, Boolean> = mutableMapOf(),
    var timeouts: MutableMap<String, Long> = mutableMapOf(),
    var limits: MutableMap<String, Int> = mutableMapOf()
) {
    // 설정 변경 리스너
    private val listeners = mutableListOf<ConfigChangeListener>()

    fun addListener(listener: ConfigChangeListener) {
        listeners.add(listener)
    }

    fun updateFeatureFlag(key: String, value: Boolean) {
        featureFlags[key] = value
        notifyListeners("featureFlag", key, value)
    }

    fun updateTimeout(key: String, value: Long) {
        timeouts[key] = value
        notifyListeners("timeout", key, value)
    }

    fun updateLimit(key: String, value: Int) {
        limits[key] = value
        notifyListeners("limit", key, value)
    }

    private fun notifyListeners(type: String, key: String, value: Any) {
        listeners.forEach { it.onConfigChanged(type, key, value) }
    }
}

interface ConfigChangeListener {
    fun onConfigChanged(type: String, key: String, value: Any)
}
