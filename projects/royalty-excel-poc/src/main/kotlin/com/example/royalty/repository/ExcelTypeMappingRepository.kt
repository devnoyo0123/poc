package com.example.royalty.repository

import com.example.royalty.entity.ExcelTypeMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExcelTypeMappingRepository : JpaRepository<ExcelTypeMapping, Long> {
    fun findByType(type: String): ExcelTypeMapping?
}
