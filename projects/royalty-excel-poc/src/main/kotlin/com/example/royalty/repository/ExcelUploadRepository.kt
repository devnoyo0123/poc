package com.example.royalty.repository

import com.example.royalty.entity.ExcelUpload
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ExcelUploadRepository : JpaRepository<ExcelUpload, UUID> {
}
