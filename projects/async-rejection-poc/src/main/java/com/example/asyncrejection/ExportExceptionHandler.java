package com.example.asyncrejection;

import java.util.Map;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ExportExceptionHandler {

    @ExceptionHandler(TaskRejectedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    Map<String, Object> handleTaskRejected(TaskRejectedException exception) {
        return Map.of(
                "exception", exception.getClass().getSimpleName(),
                "requestThread", Thread.currentThread().getName(),
                "message", "submit rejected before async handoff");
    }
}
