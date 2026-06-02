package com.example.royalty.entity

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "excel_type_mappings")
class ExcelTypeMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val type: String,

    @Column(name = "type_name", nullable = false)
    val typeName: String,

    @Column(name = "header_row_start", nullable = false)
    val headerRowStart: Int,

    @Column(name = "target_table")
    val targetTable: String?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_mappings", nullable = false)
    val columnMappings: Map<String, String>,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
