package com.paystack.payment.controller;

import com.paystack.payment.dto.PaymentIntentRequest;
import com.paystack.payment.dto.PaymentIntentResponse;
import com.paystack.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST Controller for Paystack Payment API.
 *
 * This controller exposes the payment service via HTTP endpoints,
 * allowing other backend services to call it via WebClient without
 * needing to add paystack-payment-starter as a dependency.
 *
 * Other services only need:
 * 1. The PaymentIntentRequest/Response DTOs (can be shared or duplicated)
 * 2. A WebClient to make HTTP calls
 *
 * Base URL: http://payment-service:8080/api/payments
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initialize a Paystack payment session.
     *
     * POST /api/payments/initialize
     *
     * Request body:
     * {
     *   "amount": 50000,
     *   "currency": "KES",
     *   "email": "customer@example.com",
     *   "description": "Order details",
     *   "callbackUrl": "https://yourapp.com/callback" (optional)
     * }
     *
     * Response:
     * {
     *   "data": {
     *     "clientSecret": "AC_xxx",
     *     "paymentIntentId": "ORDER-uuid",
     *     "status": "initialized",
     *     "authorizationUrl": "https://checkout.paystack.com/..."
     *   },
     *   "message": "Payment initialized successfully",
     *   "success": true
     * }
     *
     * @param request Payment initialization request
     * @return Payment initialization response with authorization URL
     */
    @PostMapping("/initialize")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<PaymentIntentResponse>> initializePayment(
        @Valid @RequestBody PaymentIntentRequest request
    ) {
        log.info("Payment initialization request - Amount: {}, Currency: {}, Email: {}",
            request.getAmount(), request.getCurrency(), request.getEmail());

        return paymentService.initializePayment(request)
            .map(paystackResponse -> {
                PaymentIntentResponse response = paymentService.toPaymentIntentResponse(paystackResponse);
                log.info("Payment initialized successfully - Reference: {}", response.getPaymentIntentId());
                return ApiResponse.success("Payment initialized successfully", response);
            })
            .onErrorResume(e -> {
                log.error("Error initializing payment", e);
                return Mono.just(ApiResponse.<PaymentIntentResponse>error("Payment initialization failed: " + e.getMessage()));
            });
    }

    /**
     * Verify a payment transaction.
     *
     * GET /api/payments/verify?reference=ORDER-uuid
     *
     * Response:
     * {
     *   "data": true,
     *   "message": "Payment verified successfully",
     *   "success": true
     * }
     *
     * @param reference Transaction reference from Paystack
     * @return Boolean indicating if payment was successful
     */
    @GetMapping("/verify")
    public Mono<ApiResponse<Boolean>> verifyPayment(@RequestParam String reference) {
        log.info("Payment verification request - Reference: {}", reference);

        return paymentService.verifyPaymentStatus(reference)
            .map(isSuccessful -> {
                if (isSuccessful) {
                    log.info("Payment verified successfully - Reference: {}", reference);
                    return ApiResponse.success("Payment verified successfully", true);
                } else {
                    log.warn("Payment verification failed - Reference: {}", reference);
                    return ApiResponse.<Boolean>error("Payment verification failed");
                }
            })
            .onErrorResume(e -> {
                log.error("Error verifying payment - Reference: {}", reference, e);
                return Mono.just(ApiResponse.<Boolean>error("Payment verification failed: " + e.getMessage()));
            });
    }

    /**
     * Handle Paystack callback after payment.
     *
     * GET /api/payments/callback?reference=ORDER-uuid
     *
     * This is called by Paystack after the user completes payment.
     * Can redirect to frontend with payment status.
     *
     * @param reference Transaction reference
     * @return Redirect response or confirmation message
     */
    @GetMapping("/callback")
    public Mono<String> paystackCallback(@RequestParam String reference) {
        log.info("Paystack callback received - Reference: {}", reference);

        return paymentService.verifyPaymentStatus(reference)
            .map(success -> {
                if (success) {
                    log.info("Callback: Payment successful - Reference: {}", reference);
                    return "Payment successful! Reference: " + reference;
                } else {
                    log.warn("Callback: Payment failed - Reference: {}", reference);
                    return "Payment failed. Reference: " + reference;
                }
            })
            .onErrorResume(e -> {
                log.error("Error processing callback - Reference: {}", reference, e);
                return Mono.just("Error processing payment callback: " + e.getMessage());
            });
    }

    /**
     * Get payment details (advanced endpoint).
     *
     * GET /api/payments/details?reference=ORDER-uuid
     *
     * Returns detailed payment information from Paystack.
     *
     * @param reference Transaction reference
     * @return Detailed payment information
     */
    @GetMapping("/details")
    public Mono<ApiResponse<?>> getPaymentDetails(@RequestParam String reference) {
        log.info("Payment details request - Reference: {}", reference);

        return paymentService.verifyPayment(reference)
            .<ApiResponse<?>>map(paystackResponse -> ApiResponse.success("Payment details retrieved", paystackResponse))
            .doOnError(e -> log.error("Error retrieving payment details - Reference: {}", reference, e))
            .onErrorResume(e -> Mono.just(ApiResponse.error("Failed to retrieve payment details")));
    }

    /**
     * Health check endpoint.
     *
     * GET /api/payments/health
     *
     * Simple endpoint to verify the payment service is running.
     *
     * @return Service status
     */
    @GetMapping("/health")
    public Mono<ApiResponse<String>> health() {
        return Mono.just(ApiResponse.success("Paystack payment service is running", "OK"));
    }
}

