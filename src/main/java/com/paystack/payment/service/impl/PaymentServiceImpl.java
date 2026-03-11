package com.paystack.payment.service.impl;

import com.paystack.payment.dto.*;
import com.paystack.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Qualifier("paystackWebClient")
    private final WebClient paystackWebClient;


    @Value("${paystack.callback.url}")
    private String callbackUrl;

    /**
     * Initialize Paystack transaction — auth is enforced by AuthWebFilter upstream
     */
    @Override
    @CircuitBreaker(name = "paystack", fallbackMethod = "initializeFallback")
    @Retry(name = "paystack")
    public Mono<ApiResponse<PaymentIntentResponse>> initializePaymentAuthenticated(PaymentIntentRequest request) {

        return initializePayment(request)
                .map(this::toPaymentIntentResponse)
                .map(resp -> ApiResponse.success("Payment initialized successfully", resp))
                .onErrorResume(e -> handleError("Payment initialization failed", e));
    }

    /**
     * Core Paystack initialization
     */
    private Mono<PaystackInitializeResponse> initializePayment(PaymentIntentRequest request) {

        String reference = "PAY-" + UUID.randomUUID();

        PaystackInitializeRequest paystackRequest = PaystackInitializeRequest.builder()
                .amount(request.getAmount())
                .email(request.getEmail())
                .currency(request.getCurrency().toUpperCase())
                .reference(reference)
                .callbackUrl(resolveCallbackUrl(request))
                .channels(List.of("card", "mobile_money", "ussd", "bank"))
                .build();

        log.info("Initializing Paystack payment ref={}", reference);

        return paystackWebClient.post()
                .uri("/transaction/initialize")
                .bodyValue(paystackRequest)
                .retrieve()
                .bodyToMono(PaystackInitializeResponse.class)
                .doOnSuccess(res ->
                        log.info("Paystack initialization success ref={}", reference))
                .doOnError(err ->
                        log.error("Paystack initialization failed ref={}", reference, err));
    }

    /**
     * Verify payment — auth is enforced by AuthWebFilter upstream
     */
    @Override
    public Mono<ApiResponse<Boolean>> verifyPaymentAuthenticated(VerifyPaymentRequest request) {

        return verifyPaymentStatus(request.getReference())
                .map(success -> success
                        ? ApiResponse.success("Payment verified successfully", true)
                        : ApiResponse.<Boolean>error("Payment not successful"))
                .onErrorResume(e -> handleError("Payment verification error", e));
    }

    /**
     * Verify Paystack transaction
     */
    @Override
    public Mono<PaystackVerifyResponse> verifyPayment(String reference) {

        return paystackWebClient.get()
                .uri("/transaction/verify/{reference}", reference)
                .retrieve()
                .bodyToMono(PaystackVerifyResponse.class)
                .doOnSuccess(response -> {
                    if (response.isStatus()) {
                        log.info("Paystack verification success reference={}", reference);
                    } else {
                        log.warn("Paystack verification failed reference={}", reference);
                    }
                })
                .doOnError(error ->
                        log.error("Error verifying payment reference={}", reference, error));
    }

    /**
     * Check if payment was successful
     */
    @Override
    public Mono<Boolean> verifyPaymentStatus(String reference) {

        return verifyPayment(reference)
                .map(response ->
                        response.isStatus()
                                && response.getData() != null
                                && "success".equalsIgnoreCase(response.getData().getStatus()))
                .doOnSuccess(status ->
                        log.info("Payment verification ref={} status={}", reference, status))
                .onErrorReturn(false);
    }

    /**
     * Handle Paystack callback (no auth required)
     */
    @Override
    public Mono<String> handleCallback(String reference) {
        log.info("Paystack callback received - Reference: {}", reference);
        return verifyPaymentStatus(reference)
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
     * Get full payment details (no auth required)
     */
    @Override
    public Mono<ApiResponse<PaystackVerifyResponse>> getPaymentDetails(String reference) {
        log.info("Payment details request - Reference: {}", reference);
        return verifyPayment(reference)
                .map(response -> ApiResponse.success("Payment details retrieved", response))
                .doOnError(e -> log.error("Error retrieving payment details - Reference: {}", reference, e))
                .onErrorResume(e -> Mono.just(ApiResponse.error("Failed to retrieve payment details")));
    }

    /**
     * Convert Paystack response to internal payment response
     */
    @Override
    public PaymentIntentResponse toPaymentIntentResponse(PaystackInitializeResponse response) {

        if (!response.isStatus() || response.getData() == null) {
            throw new RuntimeException(
                    "Failed to initialize payment: " + response.getMessage());
        }

        return PaymentIntentResponse.builder()
                .paymentIntentId(response.getData().getReference())
                .authorizationUrl(response.getData().getAuthorizationUrl())
                .clientSecret(response.getData().getAccessCode())
                .status("initialized")
                .build();
    }

    /**
     * Resolve callback URL
     */
    private String resolveCallbackUrl(PaymentIntentRequest request) {

        return request.getCallbackUrl() != null
                ? request.getCallbackUrl()
                : callbackUrl;
    }

    /**
     * Circuit breaker fallback
     */
    private Mono<ApiResponse<PaymentIntentResponse>> initializeFallback(
            PaymentIntentRequest request, Throwable ex) {

        log.error("Paystack service unavailable", ex);

        return Mono.just(
                ApiResponse.error("Payment service temporarily unavailable"));
    }

    /**
     * Generic error handler
     */
    private <T> Mono<ApiResponse<T>> handleError(String message, Throwable e) {

        log.error(message, e);

        return Mono.just(
                ApiResponse.error(e.getMessage())
        );
    }
}