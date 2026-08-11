package com.example.lab.domain.auth

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class UserTypeSpec: DescribeSpec({
   describe("UserType enum") {
       it("ADMIN, WRITER 두 값을 가진다.") {
           UserType.entries.map { it.name }.toSet() shouldBe setOf("ADMIN", "WRITER")
       }
   }
})
