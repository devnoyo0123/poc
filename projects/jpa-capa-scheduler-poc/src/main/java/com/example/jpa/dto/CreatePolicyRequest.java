package com.example.jpa.dto;

import com.example.jpa.enums.RepeatType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
public class CreatePolicyRequest {
    private Long warehouseId;
    private RepeatType repeatType;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startAt;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endAt;

    private Long createdBy;
}
