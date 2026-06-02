package com.example.webfluxsselab.repository

import com.example.webfluxsselab.model.Department
import com.example.webfluxsselab.model.UserWithDepartment
import io.r2dbc.spi.Row
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

@Repository
class JooqUserRepository(
    private val client: DatabaseClient
) {

    suspend fun findUserWithDepartment(userId: Long): UserWithDepartment? {
        // Use plain SQL that works with R2DBC
        val sql = """
            SELECT u.id, u.name, u.email,
                   d.id as dept_id, d.name as dept_name, d.user_id as dept_user_id
            FROM users u
            LEFT JOIN departments d ON u.id = d.user_id
            WHERE u.id = $1
        """.trimIndent()

        return client.sql(sql)
            .bind(0, userId)
            .map { row, _ -> mapRowToUserWithDepartment(row) }
            .one()
            .awaitSingleOrNull()
    }

    private fun mapRowToUserWithDepartment(row: Row): UserWithDepartment {
        // Access columns by name to avoid type issues
        val userId = row.get("id", Long::class.java)!!
        val userName = row.get("name", String::class.java)!!
        val userEmail = row.get("email", String::class.java)!!

        val deptId = row.get("dept_id", Long::class.java)
        val department = if (deptId != null) {
            val deptName = row.get("dept_name", String::class.java)!!
            val deptUserId = row.get("dept_user_id", Long::class.java)
            Department(id = deptId, name = deptName, userId = deptUserId)
        } else null

        return UserWithDepartment(
            id = userId,
            name = userName,
            email = userEmail,
            department = department
        )
    }
}
