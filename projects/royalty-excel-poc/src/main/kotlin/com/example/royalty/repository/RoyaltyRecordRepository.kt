package com.example.royalty.repository

import com.example.royalty.entity.RoyaltyRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RoyaltyRecordRepository : JpaRepository<RoyaltyRecord, Long> {
    fun findByUploadId(uploadId: UUID): List<RoyaltyRecord>
    fun deleteByUploadId(uploadId: UUID)
}
