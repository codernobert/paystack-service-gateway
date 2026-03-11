package com.paystack.payment.controller;

import com.paystack.payment.dto.ApiResponse;
import com.paystack.payment.dto.PaymentIntentRequest;
import com.paystack.payment.dto.PaymentIntentResponse;
import com.paystack.payment.dto.PaystackVerifyResponse;
import com.paystack.payment.dto.VerifyPaymentRequest;
import com.paystack.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CheckoutController {

    private final PaymentService paymentService;

    @PostMapping("/initialize")
    public Mono<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequest request) {

        return paymentService.initializePaymentAuthenticated(request);
    }

    @PostMapping("/verify")
    public Mono<ApiResponse<Boolean>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {

        return paymentService.verifyPaymentAuthenticated(request);
    }

    /**
     * Handle Paystack callback after payment.
     * GET /api/payments/callback?reference=ORDER-uuid
     * Called by Paystack after the user completes payment (no auth required).
     */
    @GetMapping("/callback")
    public Mono<String> paystackCallback(@RequestParam String reference) {
        return paymentService.handleCallback(reference);
    }

    /**
     * Get payment details.
     * GET /api/payments/details?reference=ORDER-uuid
     */
    @GetMapping("/details")
    public Mono<ApiResponse<PaystackVerifyResponse>> getPaymentDetails(@RequestParam String reference) {
        return paymentService.getPaymentDetails(reference);
    }

    /**
     * Health check endpoint.
     * GET /api/payments/health
     */
    @GetMapping("/health")
    public Mono<ApiResponse<String>> health() {
        return Mono.just(ApiResponse.success("Paystack payment service is running", "OK"));
    }
}
