package com.example.jpa.embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;

/**
 * 마감 기간(시작 ~ 종료)을 표현하는 제네릭 @Embeddable
 *
 * @param <T> 시간 타입 (LocalTime, LocalDateTime 등)
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CutOffPeriod<T extends Temporal & Comparable<? super T>> {

    @Column(name = "start_at", nullable = false)
    private T startAt;

    @Column(name = "end_at", nullable = false)
    private T endAt;

    /**
     * 기간이 유효한지 검증 (시작 시간이 종료 시간보다 이전인지)
     */
    public boolean isValid() {
        if (startAt == null || endAt == null) {
            return false;
        }
        return startAt.compareTo(endAt) < 0;
    }

    /**
     * 주어진 시간이 이 기간 내에 포함되는지 확인
     */
    public boolean contains(T time) {
        if (time == null || !isValid()) {
            return false;
        }
        return time.compareTo(startAt) >= 0 && time.compareTo(endAt) <= 0;
    }
}
