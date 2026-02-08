package com.paystack.payment.service;

import com.paystack.payment.config.PaystackConfigProperties;
import com.paystack.payment.dto.PaymentIntentRequest;
import com.paystack.payment.dto.PaymentIntentResponse;
import com.paystack.payment.dto.internal.PaystackInitializeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 * Tests the payment initialization, verification, and conversion methods.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private PaymentService paymentService;

    @Mock
    private PaystackConfigProperties config;

    @BeforeEach
    void setUp() {
        // Setup mock config
        PaystackConfigProperties.Api api = new PaystackConfigProperties.Api();
        api.setKey("sk_test_123456789");
        api.setUrl("https://api.paystack.co");

        PaystackConfigProperties.Callback callback = new PaystackConfigProperties.Callback();
        callback.setUrl("http://localhost:8080/callback");

        when(config.getApi()).thenReturn(api);
        when(config.getCallback()).thenReturn(callback);

        paymentService = new PaymentService(config);
    }

    @Test
    void testPaymentIntentResponseConversion() {
        // Arrange
        PaystackInitializeResponse.PaystackData data = new PaystackInitializeResponse.PaystackData();
        data.setAccessCode("AC123");
        data.setReference("ORDER-abc123");
        data.setAuthorizationUrl("https://checkout.paystack.com/token");

        PaystackInitializeResponse paystackResponse = new PaystackInitializeResponse();
        paystackResponse.setStatus(true);
        paystackResponse.setData(data);

        // Act
        PaymentIntentResponse response = paymentService.toPaymentIntentResponse(paystackResponse);

        // Assert
        assertNotNull(response);
        assertEquals("AC123", response.getClientSecret());
        assertEquals("ORDER-abc123", response.getPaymentIntentId());
        assertEquals("initialized", response.getStatus());
        assertEquals("https://checkout.paystack.com/token", response.getAuthorizationUrl());
    }

    @Test
    void testPaymentIntentResponseConversionWithNullData() {
        // Arrange
        PaystackInitializeResponse paystackResponse = new PaystackInitializeResponse();
        paystackResponse.setStatus(true);
        paystackResponse.setData(null);

        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> paymentService.toPaymentIntentResponse(paystackResponse));
    }

    @Test
    void testPaymentIntentResponseConversionWithFailureStatus() {
        // Arrange
        PaystackInitializeResponse paystackResponse = new PaystackInitializeResponse();
        paystackResponse.setStatus(false);
        paystackResponse.setMessage("Invalid request");

        // Act & Assert
        assertThrows(RuntimeException.class,
            () -> paymentService.toPaymentIntentResponse(paystackResponse));
    }

    @Test
    void testCreatePaymentRequest() {
        // Arrange
        PaymentIntentRequest request = PaymentIntentRequest.builder()
            .amount(50000L)
            .currency("KES")
            .email("test@example.com")
            .description("Test payment")
            .build();

        // Assert
        assertNotNull(request);
        assertEquals(50000L, request.getAmount());
        assertEquals("KES", request.getCurrency());
        assertEquals("test@example.com", request.getEmail());
        assertEquals("Test payment", request.getDescription());
    }

    @Test
    void testCreatePaymentRequestWithCustomCallback() {
        // Arrange
        PaymentIntentRequest request = PaymentIntentRequest.builder()
            .amount(100000L)
            .currency("NGN")
            .email("user@example.com")
            .callbackUrl("https://custom.callback.url")
            .build();

        // Assert
        assertNotNull(request);
        assertEquals("https://custom.callback.url", request.getCallbackUrl());
    }
}

