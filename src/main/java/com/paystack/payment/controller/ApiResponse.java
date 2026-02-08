package com.paystack.payment.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper for all REST endpoints.
 *
 * Used to provide a consistent response format across all payment service endpoints.
 *
 * Example successful response:
 * {
 *   "data": {...},
 *   "message": "Operation successful",
 *   "success": true
 * }
 *
 * Example error response:
 * {
 *   "data": null,
 *   "message": "Error message",
 *   "success": false
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private String message;
    private boolean success;

    /**
     * Create a successful response.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .data(data)
            .message(message)
            .success(true)
            .build();
    }

    /**
     * Create an error response.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .data(null)
            .message(message)
            .success(false)
            .build();
    }

    /**
     * Create a response with only a message (no data).
     */
    public static <T> ApiResponse<T> message(String message) {
        return ApiResponse.<T>builder()
            .message(message)
            .success(true)
            .build();
    }
}

