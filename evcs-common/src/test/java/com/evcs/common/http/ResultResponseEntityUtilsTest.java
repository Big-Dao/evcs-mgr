package com.evcs.common.http;

import com.evcs.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultResponseEntityUtilsTest {

    @Test
    @DisplayName("isSuccess - 2xx 且 Result.success 时应返回 true")
    void testIsSuccess_shouldReturnTrue_when2xxAndResultSuccess() {
        // Arrange
        ResponseEntity<Result<String>> response = ResponseEntity.ok(Result.success("ok", "ok"));

        // Act
        boolean success = ResultResponseEntityUtils.isSuccess(response);

        // Assert
        assertTrue(success, "2xx 且 Result.success 应视为成功");
    }

    @Test
    @DisplayName("isSuccess - 非 2xx 时应返回 false")
    void testIsSuccess_shouldReturnFalse_whenNon2xx() {
        // Arrange
        ResponseEntity<Result<String>> response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.success("ok", "ok"));

        // Act
        boolean success = ResultResponseEntityUtils.isSuccess(response);

        // Assert
        assertFalse(success, "非 2xx 即使 body 成功也不应视为成功");
    }

    @Test
    @DisplayName("isSuccess - Result.failure 时应返回 false")
    void testIsSuccess_shouldReturnFalse_whenResultFailure() {
        // Arrange
        ResponseEntity<Result<String>> response = ResponseEntity.ok(Result.failure("fail"));

        // Act
        boolean success = ResultResponseEntityUtils.isSuccess(response);

        // Assert
        assertFalse(success, "Result.failure 不应视为成功");
    }

    @Test
    @DisplayName("isSuccess(expectedStatus) - 非预期状态码时应返回 false")
    void testIsSuccess_shouldReturnFalse_whenUnexpectedStatus() {
        // Arrange
        ResponseEntity<Result<String>> response = ResponseEntity.status(HttpStatus.CREATED)
            .body(Result.success("ok", "ok"));

        // Act
        boolean success = ResultResponseEntityUtils.isSuccess(response, HttpStatus.OK);

        // Assert
        assertFalse(success, "状态码不等于预期值时不应视为成功");
    }

    @Test
    @DisplayName("dataIfSuccess - 失败响应应返回 null")
    void testDataIfSuccess_shouldReturnNull_whenNotSuccess() {
        // Arrange
        ResponseEntity<Result<String>> response = ResponseEntity.ok(Result.failure("fail"));

        // Act
        String data = ResultResponseEntityUtils.dataIfSuccess(response);

        // Assert
        assertNull(data, "失败响应应返回 null 数据");
    }
}
