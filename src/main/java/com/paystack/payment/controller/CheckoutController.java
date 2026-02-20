package com.paystack.payment.controller;

import com.paystack.payment.dto.ApiResponse;
import com.paystack.payment.dto.PaymentIntentRequest;
import com.paystack.payment.dto.PaymentIntentResponse;
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
        @Valid @RequestBody PaymentIntentRequest request
    ) {
        return paymentService.initializePayment(request)
            .map(paystackResponse -> {
                PaymentIntentResponse response = paymentService.toPaymentIntentResponse(paystackResponse);
                return ApiResponse.success("Payment initialized successfully", response);
            })
            .onErrorResume(e -> Mono.just(ApiResponse.error(e.getMessage())));
    }

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
