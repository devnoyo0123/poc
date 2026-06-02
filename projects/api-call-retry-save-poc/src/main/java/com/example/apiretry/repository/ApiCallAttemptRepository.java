package com.example.apiretry.repository;

import com.example.apiretry.entity.ApiCallAttempt;
import com.example.apiretry.entity.ApiCallResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiCallAttemptRepository extends JpaRepository<ApiCallAttempt, Long> {
    List<ApiCallAttempt> findByResultOrderByAttemptTimeAsc(ApiCallResult result);
    List<ApiCallAttempt> findByResultOrderByAttemptNumberAsc(ApiCallResult result);
}
