package com.paystack.payment.service;

import com.paystack.payment.dto.*;
import reactor.core.publisher.Mono;

public interface PaymentService {

    /**
     * Initialize payment after authentication
     */
    Mono<ApiResponse<PaymentIntentResponse>> initializePaymentAuthenticated(
            PaymentIntentRequest request
    );

    /**
     * Verify payment after authentication
     */
    Mono<ApiResponse<Boolean>> verifyPaymentAuthenticated(
            VerifyPaymentRequest request
    );

    /**
     * Verify Paystack transaction
     */
    Mono<PaystackVerifyResponse> verifyPayment(String reference);

    /**
     * Check if payment status is successful
     */
    Mono<Boolean> verifyPaymentStatus(String reference);

    /**
     * Handle Paystack callback (no auth required)
     */
    Mono<String> handleCallback(String reference);

    /**
     * Get full payment details (no auth required)
     */
    Mono<ApiResponse<PaystackVerifyResponse>> getPaymentDetails(String reference);

    /**
     * Convert Paystack response to internal response
     */
    PaymentIntentResponse toPaymentIntentResponse(
            PaystackInitializeResponse paystackResponse
    );
}