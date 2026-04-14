package com.graduation.common;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public GlobalResponse<?> handleRuntimeException(RuntimeException e) {
        return GlobalResponse.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public GlobalResponse<?> handleException(Exception e) {
        return GlobalResponse.error("服务器内部错误：" + e.getMessage());
    }
}
