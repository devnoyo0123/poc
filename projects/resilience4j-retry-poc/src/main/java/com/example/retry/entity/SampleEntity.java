package com.example.retry.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "sample")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer retryCount;

    public SampleEntity(String name) {
        this.name = name;
        this.retryCount = 0;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
