package com.example.avro.model

import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord

data class ConfigChangeEvent(
    val configType: String,  // featureFlag, timeout, limit
    val key: String,
    val value: Any,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String? = null
)

fun ConfigChangeEvent.toGenericRecord(): GenericRecord {
    val schemaStr = """
    {
      "type": "record",
      "name": "ConfigChangeEvent",
      "namespace": "com.example.avro",
      "fields": [
        {"name": "configType", "type": "string"},
        {"name": "key", "type": "string"},
        {"name": "value", "type": ["string", "long", "int", "boolean"]},
        {"name": "timestamp", "type": "long"},
        {"name": "source", "type": ["null", "string"], "default": null}
      ]
    }
    """

    val schema = org.apache.avro.Schema.Parser().parse(schemaStr)
    val record = GenericData.Record(schema)

    record.put("configType", configType)
    record.put("key", key)

    // value 타입에 따라 변환
    when (value) {
        is String -> record.put("value", value)
        is Long -> record.put("value", value)
        is Int -> record.put("value", value.toLong())
        is Boolean -> record.put("value", value.toString())
        else -> record.put("value", value.toString())
    }

    record.put("timestamp", timestamp)
    record.put("source", source)

    return record
}
