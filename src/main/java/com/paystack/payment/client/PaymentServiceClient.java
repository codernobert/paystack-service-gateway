package com.paystack.payment.client;

import com.paystack.payment.controller.ApiResponse;
import com.paystack.payment.dto.PaymentIntentRequest;
import com.paystack.payment.dto.PaymentIntentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * WebClient for calling the Paystack Payment Service from other backend services.
 *
 * This allows other microservices to interact with the payment service via REST API
 * without needing to add paystack-payment-starter as a dependency.
 *
 * Usage in other services:
 *
 * @Bean
 * public PaymentServiceClient paymentServiceClient() {
 *     return new PaymentServiceClient("http://payment-service:8080");
 * }
 *
 * @Autowired
 * private PaymentServiceClient paymentClient;
 *
 * public void makePayment() {
 *     PaymentIntentRequest request = new PaymentIntentRequest(...);
 *     paymentClient.initializePayment(request)
 *         .subscribe(response -> { ... });
 * }
 */
@Component
@Slf4j
public class PaymentServiceClient {

    private final WebClient webClient;
    private static final String PAYMENT_SERVICE_URL = "${payment.service.url:http://localhost:8080}";

    public PaymentServiceClient(@Qualifier("paymentServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Initialize a payment via the payment service.
     *
     * @param request Payment initialization request
     * @return Payment initialization response with authorization URL
     */
    public Mono<PaymentIntentResponse> initializePayment(PaymentIntentRequest request) {
        log.info("Calling payment service to initialize payment - Amount: {}, Currency: {}",
            request.getAmount(), request.getCurrency());

        return webClient.post()
            .uri("/api/payments/initialize")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> {
                if (response.isSuccess()) {
                    // Parse the response data as PaymentIntentResponse
                    Object data = response.getData();
                    if (data instanceof PaymentIntentResponse) {
                        return (PaymentIntentResponse) data;
                    }
                    // Handle case where response.data is a Map (from JSON deserialization)
                    log.error("Unexpected response format from payment service");
                    throw new RuntimeException("Invalid response format from payment service");
                } else {
                    log.error("Payment service returned error: {}", response.getMessage());
                    throw new RuntimeException("Payment service error: " + response.getMessage());
                }
            })
            .doOnError(e -> log.error("Error initializing payment", e));
    }

    /**
     * Verify a payment via the payment service.
     *
     * @param reference Transaction reference
     * @return Boolean indicating if payment was successful
     */
    public Mono<Boolean> verifyPayment(String reference) {
        log.info("Calling payment service to verify payment - Reference: {}", reference);

        return webClient.get()
            .uri("/api/payments/verify?reference={reference}", reference)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> {
                if (response.isSuccess()) {
                    Object data = response.getData();
                    if (data instanceof Boolean) {
                        return (Boolean) data;
                    }
                    // Handle case where response.data is parsed as different type
                    log.error("Unexpected response format from payment service");
                    throw new RuntimeException("Invalid response format from payment service");
                } else {
                    log.warn("Payment verification failed: {}", response.getMessage());
                    return false;
                }
            })
            .onErrorResume(e -> {
                log.error("Error verifying payment", e);
                return Mono.just(false);
            });
    }

    /**
     * Get detailed payment information via the payment service.
     *
     * @param reference Transaction reference
     * @return Detailed payment information
     */
    public Mono<Object> getPaymentDetails(String reference) {
        log.info("Calling payment service to get payment details - Reference: {}", reference);

        return webClient.get()
            .uri("/api/payments/details?reference={reference}", reference)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> {
                if (response.isSuccess()) {
                    return response.getData();
                } else {
                    log.error("Failed to get payment details: {}", response.getMessage());
                    throw new RuntimeException("Failed to get payment details: " + response.getMessage());
                }
            })
            .doOnError(e -> log.error("Error getting payment details", e));
    }

    /**
     * Check if the payment service is healthy.
     *
     * @return Health status
     */
    public Mono<Boolean> health() {
        log.debug("Checking payment service health");

        return webClient.get()
            .uri("/api/payments/health")
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(ApiResponse::isSuccess)
            .onErrorResume(e -> {
                log.error("Payment service health check failed", e);
                return Mono.just(false);
            });
    }
}

