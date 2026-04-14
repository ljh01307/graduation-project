package com.graduation.common;

public class GlobalResponse<T> {
    private int code;
    private String message;
    private T data;

    public GlobalResponse() {}

    public GlobalResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static <T> GlobalResponse<T> success(T data) {
        return new GlobalResponse<>(200, "成功", data);
    }

    public static <T> GlobalResponse<T> success(String message, T data) {
        return new GlobalResponse<>(200, message, data);
    }

    public static <T> GlobalResponse<T> success() {
        return new GlobalResponse<>(200, "成功", null);
    }

    public static <T> GlobalResponse<T> error(String message) {
        return new GlobalResponse<>(400, message, null);
    }

    public static <T> GlobalResponse<T> error(int code, String message) {
        return new GlobalResponse<>(code, message, null);
    }
}
