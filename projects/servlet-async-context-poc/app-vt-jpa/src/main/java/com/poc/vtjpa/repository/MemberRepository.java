package com.poc.vtjpa.repository;

import com.poc.vtjpa.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * PostgreSQL 전용 slow query 포함.
 * pg_sleep 은 H2 에서 동작하지 않음 — PostgreSQL 프로파일 필수.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query(value = "SELECT m.* FROM members m, (SELECT pg_sleep(:seconds) AS s) AS sub ORDER BY m.id LIMIT 1",
            nativeQuery = true)
    Member findFirstSlow(@Param("seconds") double seconds);

    default Member findFirstSlow() {
        return findFirstSlow(1.0d);
    }
}
