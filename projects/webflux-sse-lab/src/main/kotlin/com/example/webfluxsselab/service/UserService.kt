package com.example.webfluxsselab.service

import com.example.webfluxsselab.model.User
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class UserService {

    private val users = ConcurrentHashMap<Long, User>()

    init {
        // 초기 데이터
        users[1] = User(1, "John Doe", "john@example.com")
        users[2] = User(2, "Jane Smith", "jane@example.com")
        users[3] = User(3, "Bob Johnson", "bob@example.com")
    }

    suspend fun findById(id: Long): User? {
        delay(100)  // DB 조회 시뮬레이션
        return users[id]
    }

    suspend fun findAll(): List<User> {
        delay(200)  // DB 조회 시뮬레이션
        return users.values.toList()
    }

    suspend fun save(user: User): User {
        delay(100)  // DB 저장 시뮬레이션
        users[user.id] = user
        return user
    }
}
