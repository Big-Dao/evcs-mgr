package com.evcs.common.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("业务异常 - HTTP 标准状态码应透传")
    void testHandleBusinessException_shouldSetHttpStatus_whenStandardHttpCode() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);
        BusinessException exception = new BusinessException(401, "未登录");

        // Act
        handler.handleBusinessException(exception, response);

        // Assert
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("业务异常 - 非标准业务码应返回 400")
    void testHandleBusinessException_shouldSetBadRequest_whenNonStandardCode() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);
        BusinessException exception = new BusinessException(4101, "用户不存在");

        // Act
        handler.handleBusinessException(exception, response);

        // Assert
        verify(response).setStatus(400);
    }

    @Test
    @DisplayName("业务异常 - 500 应映射为 500")
    void testHandleBusinessException_shouldSetInternalServerError_when500() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);
        BusinessException exception = new BusinessException(500, "内部服务器错误");

        // Act
        handler.handleBusinessException(exception, response);

        // Assert
        verify(response).setStatus(500);
    }
}
