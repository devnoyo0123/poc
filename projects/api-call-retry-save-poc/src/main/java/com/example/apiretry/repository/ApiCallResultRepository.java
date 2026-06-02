package com.example.apiretry.repository;

import com.example.apiretry.entity.ApiCallResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiCallResultRepository extends JpaRepository<ApiCallResult, Long> {
    List<ApiCallResult> findByEndpointOrderByCallTimeDesc(String endpoint);
}
