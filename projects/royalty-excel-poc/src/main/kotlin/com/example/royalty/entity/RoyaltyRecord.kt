package com.example.royalty.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "royalty_records")
class RoyaltyRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "upload_id", nullable = false)
    val uploadId: UUID,

    @Column(name = "sheet_name", nullable = false)
    val sheetName: String,

    @Column(name = "row_number", nullable = false)
    val rowNumber: Int,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_data", nullable = false)
    val originalData: Map<String, Any?>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_data", nullable = false)
    val matchedData: Map<String, Any?>,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "header_mapping")
    val headerMapping: Map<String, String>? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
