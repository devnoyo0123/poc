package com.example.avro.model

import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord

data class UserEvent(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String? = null,
    val userId: String? = null,
    val metadata: EventMetadata? = null
)

data class EventMetadata(
    val source: String? = null,
    val version: String? = null
)

fun UserEvent.toGenericRecord(): GenericRecord {
    val record = GenericData.Record(
        org.apache.avro.Schema.Parser()
            .parse(javaClass.getResourceAsStream("/avro/UserEvent.avsc"))
    )

    record.put("id", id)
    record.put("timestamp", timestamp)
    record.put("eventType", eventType)
    record.put("userId", userId)

    metadata?.let { meta ->
        val metadataRecord = GenericData.Record(
            record.schema.fields[4].schema()
        )
        metadataRecord.put("source", meta.source)
        metadataRecord.put("version", meta.version)
        record.put("metadata", metadataRecord)
    } ?: record.put("metadata", null)

    return record
}
