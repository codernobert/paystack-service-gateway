package com.paystack.payment.service;

import com.paystack.payment.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {
    
    @Value("${paystack.api.key}")
    private String paystackApiKey;

    @Value("${paystack.api.url}")
    private String paystackApiUrl;

//    @Value("https://webhook.site/c430e472-8d7a-408b-90e9-e52d61a60069")
    private String callbackUrl;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        if (paystackApiKey == null || paystackApiKey.isEmpty() || paystackApiKey.contains("YOUR_PAYSTACK")) {
            log.error("=======================================================");
            log.error("PAYSTACK API KEY NOT CONFIGURED!");
            log.error("Please set a valid Paystack API key in application.properties");
            log.error("Get your key from: https://dashboard.paystack.com/#/settings/developers");
            log.error("=======================================================");
            throw new IllegalStateException("Paystack API key is not configured. Please update application.properties");
        }

        this.webClient = WebClient.builder()
            .baseUrl(paystackApiUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + paystackApiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        log.info("Paystack API initialized successfully with key: {}****", paystackApiKey.substring(0, 8));
    }
    
    /**
     * Initialize a Paystack transaction
     * This creates a payment session and returns the authorization URL for the user to complete payment
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "initializePaymentFallback")
    @Retry(name = "paymentService")
    @TimeLimiter(name = "paymentService")
    public Mono<PaystackInitializeResponse> initializePayment(PaymentIntentRequest request) {
        return Mono.fromCallable(() -> {
            log.info("Initializing Paystack payment for amount: {} {}", request.getAmount(), request.getCurrency());

            // Generate unique reference
            String reference = "PAYMENT_ID-" + UUID.randomUUID().toString();

            // Use callback URL from request if provided, otherwise use the configured one
            String effectiveCallbackUrl = (request.getCallbackUrl() != null && !request.getCallbackUrl().isEmpty())
                ? request.getCallbackUrl()
                : callbackUrl;

            log.info("Using callback URL: {}", effectiveCallbackUrl);

            // Build Paystack request
            PaystackInitializeRequest paystackRequest = PaystackInitializeRequest.builder()
                .amount(request.getAmount()) // Amount should be in smallest currency unit (kobo for NGN, cents for KES)
                .email(request.getEmail() != null ? request.getEmail() : "customer@example.com")
                .currency(request.getCurrency().toUpperCase())
                .reference(reference)
                .callbackUrl(effectiveCallbackUrl)
                .channels(List.of("card", "mobile_money", "ussd", "bank")) // Enable all payment channels including MPESA
                .build();

            return paystackRequest;
        }).flatMap(paystackRequest -> {
            log.debug("Sending request to Paystack: {}", paystackRequest);
            return webClient.post()
                .uri("/transaction/initialize")
                .bodyValue(paystackRequest)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Paystack API error response: {}", errorBody);
                            return Mono.error(new RuntimeException("Paystack API error: " + errorBody));
                        })
                )
                .bodyToMono(PaystackInitializeResponse.class)
                .doOnSuccess(response -> {
                    if (response.isStatus()) {
                        log.info("Payment initialized successfully. Reference: {}",
                            response.getData().getReference());
                        log.info("Authorization URL: {}", response.getData().getAuthorizationUrl());
                    } else {
                        log.error("Payment initialization failed: {}", response.getMessage());
                    }
                })
                .doOnError(e -> log.error("Error initializing Paystack payment", e));
        });
    }
    
    /**
     * Verify a Paystack transaction
     * Call this after the user completes payment to verify the transaction status
     */
    public Mono<PaystackVerifyResponse> verifyPayment(String reference) {
        return webClient.get()
            .uri("/transaction/verify/{reference}", reference)
            .retrieve()
            .bodyToMono(PaystackVerifyResponse.class)
            .doOnSuccess(response -> {
                if (response.isStatus() && "success".equalsIgnoreCase(response.getData().getStatus())) {
                    log.info("Payment verified successfully for reference: {}", reference);
                } else {
                    log.warn("Payment verification failed for reference: {}. Status: {}",
                        reference, response.getData().getStatus());
                }
            })
            .doOnError(e -> log.error("Error verifying payment for reference: {}", reference, e));
    }
    
    /**
     * Check if payment was successful
     */
    public Mono<Boolean> verifyPaymentStatus(String reference) {
        return verifyPayment(reference)
            .map(response -> response.isStatus() &&
                "success".equalsIgnoreCase(response.getData().getStatus()))
            .onErrorReturn(false);
    }
    
    /**
     * Convert PaystackInitializeResponse to PaymentIntentResponse for backward compatibility
     */
    public PaymentIntentResponse toPaymentIntentResponse(PaystackInitializeResponse paystackResponse) {
        if (paystackResponse.isStatus() && paystackResponse.getData() != null) {
            return PaymentIntentResponse.builder()
                .clientSecret(paystackResponse.getData().getAccessCode())
                .paymentIntentId(paystackResponse.getData().getReference())
                .status("initialized")
                .authorizationUrl(paystackResponse.getData().getAuthorizationUrl())
                .build();
        }
        throw new RuntimeException("Failed to initialize payment: " + paystackResponse.getMessage());
    }

    // Fallback method for circuit breaker
    private Mono<PaystackInitializeResponse> initializePaymentFallback(PaymentIntentRequest request, Exception e) {
        log.error("Payment service fallback triggered", e);
        return Mono.error(new RuntimeException("Payment service temporarily unavailable. Please try again later."));
    }
}

