package com.example.tomcathikaritimeout;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class SlowExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SlowExceptionHandler.class);

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    Map<String, Object> handleSqlException(SQLException exception) {
        return buildBody(exception);
    }

    // JdbcTemplate translates SQLException into a DataAccessException subclass before it reaches the controller,
    // so this handler covers the HikariCP timeout path when going through JdbcTemplate.
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    Map<String, Object> handleDataAccessException(DataAccessException exception) {
        return buildBody(exception);
    }

    private Map<String, Object> buildBody(Throwable exception) {
        String requestThread = Thread.currentThread().getName();
        String rootCause = resolveRootCauseName(exception);
        String message = buildFullMessageChain(exception);

        log.info(
                "[HANDLER] thread={} exception={} rootCause={} message={}",
                requestThread,
                exception.getClass().getSimpleName(),
                rootCause,
                message);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("exception", exception.getClass().getSimpleName());
        body.put("rootCause", rootCause);
        body.put("message", message);
        body.put("requestThread", requestThread);
        return body;
    }

    // Walks the full cause chain to expose the root message (e.g. HikariCP timeout) — Spring wraps the
    // Hikari SQLTransientConnectionException as CannotGetJdbcConnectionException("Failed to obtain JDBC Connection")
    // and the real reason lives one level down in getCause().
    private String buildFullMessageChain(Throwable exception) {
        StringBuilder sb = new StringBuilder();
        Throwable current = exception;
        while (current != null) {
            if (sb.length() > 0) {
                sb.append(" | cause: ");
            }
            sb.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
            current = current.getCause();
        }
        return sb.toString();
    }

    private String resolveRootCauseName(Throwable exception) {
        if (exception instanceof SQLException sqlException && sqlException.getNextException() != null) {
            return sqlException.getNextException().getClass().getSimpleName();
        }
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof SQLException) {
                return cause.getClass().getSimpleName();
            }
            cause = cause.getCause();
        }
        return exception.getClass().getSimpleName();
    }
}
