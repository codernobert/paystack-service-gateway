# Standalone Payment Service - Example Integration

## For Services WITHOUT paystack-payment-starter Dependency

If you don't want to add `paystack-payment-starter` as a dependency, you can:
1. Just call the payment service REST API via WebClient
2. Only need to share/duplicate the DTOs

## Quick Setup

### Step 1: Create DTOs in Your Service

Copy these DTOs to your service (in `src/main/java/com/yourapp/payment/dto/`):

**PaymentIntentRequest.java**
```java
package com.yourapp.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentRequest {
    @NotNull(message = "Amount is required")
    private Long amount;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    private String description;
    private String email;
    private String callbackUrl;
}
```

**PaymentIntentResponse.java**
```java
package com.yourapp.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    private String clientSecret;
    private String paymentIntentId;
    private String status;
    private String authorizationUrl;
}
```

### Step 2: Configure Payment Service URL

**application.properties**
```properties
payment.service.url=http://payment-service:8080
```

**application.yml**
```yaml
payment:
  service:
    url: http://payment-service:8080
```

### Step 3: Create Payment Client

Create a simple client to call the payment service:

```java
package com.yourapp.payment.client;

import com.yourapp.payment.dto.PaymentIntentRequest;
import com.yourapp.payment.dto.PaymentIntentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${payment.service.url}")
    private String paymentServiceUrl;
    
    /**
     * Initialize a payment with the payment service
     */
    public Mono<PaymentIntentResponse> initializePayment(PaymentIntentRequest request) {
        log.info("Calling payment service - Amount: {}, Currency: {}", 
            request.getAmount(), request.getCurrency());
        
        return webClientBuilder
            .baseUrl(paymentServiceUrl)
            .build()
            .post()
            .uri("/api/payments/initialize")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentApiResponse.class)
            .map(response -> {
                if (response.isSuccess()) {
                    return response.getData();
                } else {
                    throw new RuntimeException("Payment service error: " + response.getMessage());
                }
            });
    }
    
    /**
     * Verify a payment with the payment service
     */
    public Mono<Boolean> verifyPayment(String reference) {
        log.info("Verifying payment - Reference: {}", reference);
        
        return webClientBuilder
            .baseUrl(paymentServiceUrl)
            .build()
            .get()
            .uri("/api/payments/verify?reference={ref}", reference)
            .retrieve()
            .bodyToMono(PaymentApiResponse.class)
            .map(response -> {
                if (response.isSuccess()) {
                    Object data = response.getData();
                    return data instanceof Boolean ? (Boolean) data : false;
                }
                return false;
            })
            .onErrorResume(e -> {
                log.error("Error verifying payment", e);
                return Mono.just(false);
            });
    }
    
    /**
     * Generic API response wrapper
     */
    @lombok.Data
    public static class PaymentApiResponse {
        private Object data;
        private String message;
        private boolean success;
    }
}
```

### Step 4: Use in Your Controller

```java
package com.yourapp.order.controller;

import com.yourapp.payment.client.PaymentServiceClient;
import com.yourapp.payment.dto.PaymentIntentRequest;
import com.yourapp.payment.dto.PaymentIntentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private PaymentServiceClient paymentClient;
    
    @Autowired
    private OrderService orderService;
    
    /**
     * Checkout endpoint
     * 1. Create order
     * 2. Initialize payment via payment service
     * 3. Return authorization URL to frontend
     */
    @PostMapping("/checkout")
    public Mono<OrderResponse> checkout(@RequestBody CheckoutRequest request) {
        return orderService.createOrder(request)
            .flatMap(order -> {
                // Create payment request
                PaymentIntentRequest paymentRequest = PaymentIntentRequest.builder()
                    .amount(order.getTotalAmount())
                    .currency("KES")
                    .email(order.getCustomerEmail())
                    .description("Order #" + order.getId())
                    .callbackUrl("https://yourapp.com/api/orders/" + order.getId() + "/payment-callback")
                    .build();
                
                // Call payment service
                return paymentClient.initializePayment(paymentRequest)
                    .map(paymentResponse -> {
                        // Update order with payment details
                        order.setPaymentReference(paymentResponse.getPaymentIntentId());
                        order.setAuthorizationUrl(paymentResponse.getAuthorizationUrl());
                        order.setPaymentStatus("PENDING");
                        return order;
                    })
                    .flatMap(orderService::saveOrder)
                    .map(this::toOrderResponse);
            });
    }
    
    /**
     * Payment callback endpoint
     * Called by payment service or Paystack redirect
     */
    @PostMapping("/{orderId}/payment-callback")
    public Mono<OrderResponse> paymentCallback(
        @PathVariable Long orderId,
        @RequestParam String reference
    ) {
        return orderService.findById(orderId)
            .flatMap(order -> paymentClient.verifyPayment(reference)
                .flatMap(isSuccessful -> {
                    if (isSuccessful) {
                        order.setPaymentStatus("COMPLETED");
                        order.setPaymentReference(reference);
                    } else {
                        order.setPaymentStatus("FAILED");
                    }
                    return orderService.saveOrder(order);
                })
            )
            .map(this::toOrderResponse);
    }
    
    /**
     * Verify payment endpoint
     */
    @GetMapping("/{orderId}/verify-payment")
    public Mono<Boolean> verifyPayment(@PathVariable Long orderId) {
        return orderService.findById(orderId)
            .flatMap(order -> paymentClient.verifyPayment(order.getPaymentReference()));
    }
    
    private OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
            .id(order.getId())
            .total(order.getTotalAmount())
            .paymentStatus(order.getPaymentStatus())
            .authorizationUrl(order.getAuthorizationUrl())
            .build();
    }
}
```

## Complete Example Service

### pom.xml (No paystack-payment-starter dependency!)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.9</version>
    </parent>

    <groupId>com.yourcompany</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Spring Data -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-r2dbc</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>r2dbc-postgresql</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### application.yml
```yaml
spring:
  application:
    name: order-service
  r2dbc:
    url: r2dbc:postgresql://localhost/orderdb
    username: postgres
    password: postgres
  webflux:
    base-path: /api

server:
  port: 8082

payment:
  service:
    url: http://localhost:8080  # Payment service URL
```

## Testing Locally

### 1. Start Payment Service
```bash
cd paystack-payment-starter
mvn spring-boot:run
```

### 2. Start Order Service
```bash
cd order-service
mvn spring-boot:run
```

### 3. Test Checkout
```bash
curl -X POST http://localhost:8082/api/orders/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{
      "productId": 1,
      "quantity": 2
    }],
    "customerEmail": "customer@example.com",
    "shippingAddress": "123 Main St"
  }'
```

### 4. Test Verify Payment
```bash
curl -X GET "http://localhost:8082/api/orders/123/verify-payment"
```

## Docker Compose Setup

```yaml
version: '3.8'

services:
  payment-service:
    build:
      context: ./paystack-payment-starter
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - PAYSTACK_API_KEY=sk_test_xxxxx
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/payments/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  order-service:
    build:
      context: ./order-service
      dockerfile: Dockerfile
    ports:
      - "8082:8082"
    depends_on:
      payment-service:
        condition: service_healthy
    environment:
      - PAYMENT_SERVICE_URL=http://payment-service:8080
```

## Benefits of This Approach

✅ **No Dependency Needed** - Other services don't need to add paystack-payment-starter to pom.xml  
✅ **Loose Coupling** - Services communicate via REST API only  
✅ **Independent Scaling** - Payment service can be scaled independently  
✅ **Shared DTOs Only** - Minimal code duplication  
✅ **Easy Deployment** - Can be deployed as separate microservice  
✅ **Clear Contract** - HTTP API is the service contract  

---

For more details, see REST_API_GUIDE.md

