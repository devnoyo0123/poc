package com.example.jpa.entity;

import com.example.jpa.embeddable.CutOffPeriod;
import com.example.jpa.enums.RepeatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseCutoffPolicyTest {

    @Test
    @DisplayName("CutOffPeriod의 isValid() 메서드는 시작 시간이 종료 시간보다 이전일 때 true를 반환한다")
    void cutOffPeriod_isValid_returnsTrue_whenStartBeforeEnd() {
        // given
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(18, 0);
        CutOffPeriod<LocalTime> period = new CutOffPeriod<>(start, end);

        // when
        boolean valid = period.isValid();

        // then
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("CutOffPeriod의 isValid() 메서드는 시작 시간이 종료 시간보다 이후일 때 false를 반환한다")
    void cutOffPeriod_isValid_returnsFalse_whenStartAfterEnd() {
        // given
        LocalTime start = LocalTime.of(18, 0);
        LocalTime end = LocalTime.of(14, 0);
        CutOffPeriod<LocalTime> period = new CutOffPeriod<>(start, end);

        // when
        boolean valid = period.isValid();

        // then
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("CutOffPeriod의 contains() 메서드는 주어진 시간이 기간 내에 있을 때 true를 반환한다")
    void cutOffPeriod_contains_returnsTrue_whenTimeIsWithinPeriod() {
        // given
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(18, 0);
        CutOffPeriod<LocalTime> period = new CutOffPeriod<>(start, end);
        LocalTime testTime = LocalTime.of(16, 0);

        // when
        boolean contains = period.contains(testTime);

        // then
        assertThat(contains).isTrue();
    }

    @Test
    @DisplayName("CutOffPeriod의 contains() 메서드는 주어진 시간이 기간 밖에 있을 때 false를 반환한다")
    void cutOffPeriod_contains_returnsFalse_whenTimeIsOutsidePeriod() {
        // given
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(18, 0);
        CutOffPeriod<LocalTime> period = new CutOffPeriod<>(start, end);
        LocalTime testTime = LocalTime.of(20, 0);

        // when
        boolean contains = period.contains(testTime);

        // then
        assertThat(contains).isFalse();
    }

    @Test
    @DisplayName("CutOffPeriod의 contains() 메서드는 경계값(시작 시간)에 대해 true를 반환한다")
    void cutOffPeriod_contains_returnsTrue_forStartBoundary() {
        // given
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(18, 0);
        CutOffPeriod<LocalTime> period = new CutOffPeriod<>(start, end);

        // when
        boolean contains = period.contains(start);

        // then
        assertThat(contains).isTrue();
    }

    @Test
    @DisplayName("CutOffPeriod의 contains() 메서드는 경계값(종료 시간)에 대해 true를 반환한다")
    void cutOffPeriod_contains_returnsTrue_forEndBoundary() {
        // given
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(18, 0);
        CutOffPeriod<LocalTime> period = new CutOffPeriod<>(start, end);

        // when
        boolean contains = period.contains(end);

        // then
        assertThat(contains).isTrue();
    }
}
