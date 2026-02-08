# Paystack Payment Service - REST API Guide

## Overview

The Paystack Payment Starter can be deployed as a **standalone microservice** that other backend services call via REST API. This approach eliminates the need for other services to add the paystack-payment-starter as a dependency.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Payment Service                          │
│                (paystack-payment-starter)                   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  REST Controllers (HTTP Endpoints)                   │   │
│  │  - POST   /api/payments/initialize                  │   │
│  │  - GET    /api/payments/verify                      │   │
│  │  - GET    /api/payments/callback                    │   │
│  │  - GET    /api/payments/details                     │   │
│  │  - GET    /api/payments/health                      │   │
│  └──────────────────────────────────────────────────────┘   │
│                          ▲                                   │
│                          │ WebClient                         │
│                          │                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  PaymentService (Business Logic)                     │   │
│  │  - initializePayment()                               │   │
│  │  - verifyPayment()                                   │   │
│  │  - verifyPaymentStatus()                             │   │
│  │  - toPaymentIntentResponse()                         │   │
│  └──────────────────────────────────────────────────────┘   │
│                          │                                   │
│                          ▼ (Paystack API)                    │
└─────────────────────────────────────────────────────────────┘
        ▲
        │ HTTP REST Calls (via WebClient)
        │ (Only PaymentIntentRequest/Response DTOs needed)
        │
┌───────┴────────┬──────────────────┬──────────────────┐
│                │                  │                  │
│  Order         │  Subscription    │  Inventory       │
│  Service       │  Service         │  Service         │
│                │                  │                  │
└────────────────┴──────────────────┴──────────────────┘
```

## Deployment

### Option 1: Standalone Microservice

Deploy paystack-payment-starter as a separate Docker container/pod:

```dockerfile
# Dockerfile for Payment Service
FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080
```

```bash
# Build
mvn clean package -f paystack-payment-starter/pom.xml

# Docker
docker build -t paystack-payment-service .
docker run -e PAYSTACK_API_KEY=sk_test_xxx -p 8080:8080 paystack-payment-service
```

### Option 2: Shared Library + REST Wrapper

Or keep it as a library but enable REST endpoints only:

```yaml
# application.yml
server:
  port: 8081

paystack:
  api:
    key: ${PAYSTACK_API_KEY}
    url: https://api.paystack.co
  callback:
    url: https://yourapi.com/payment-callback

payment:
  service:
    url: http://payment-service:8080
    connect-timeout: 5000
    read-timeout: 10000
```

## REST API Endpoints

### 1. Initialize Payment

**Request:**
```bash
POST http://payment-service:8080/api/payments/initialize
Content-Type: application/json

{
  "amount": 50000,
  "currency": "KES",
  "email": "customer@example.com",
  "description": "Order #123",
  "callbackUrl": "https://yourapp.com/payment-success" (optional)
}
```

**Response (200 Created):**
```json
{
  "data": {
    "clientSecret": "AC_abc123xyz",
    "paymentIntentId": "ORDER-uuid-12345",
    "status": "initialized",
    "authorizationUrl": "https://checkout.paystack.com/token/AC_abc123xyz"
  },
  "message": "Payment initialized successfully",
  "success": true
}
```

**Error Response (400):**
```json
{
  "data": null,
  "message": "Payment initialization failed: Invalid request",
  "success": false
}
```

---

### 2. Verify Payment

**Request:**
```bash
GET http://payment-service:8080/api/payments/verify?reference=ORDER-uuid-12345
```

**Response (200 OK):**
```json
{
  "data": true,
  "message": "Payment verified successfully",
  "success": true
}
```

**Response (200 OK - Failed):**
```json
{
  "data": false,
  "message": "Payment verification failed",
  "success": false
}
```

---

### 3. Get Payment Details

**Request:**
```bash
GET http://payment-service:8080/api/payments/details?reference=ORDER-uuid-12345
```

**Response (200 OK):**
```json
{
  "data": {
    "status": true,
    "message": "Authorization URL created",
    "data": {
      "id": 123456789,
      "reference": "ORDER-uuid-12345",
      "amount": 50000,
      "status": "success",
      "paid_at": "2025-02-08T10:30:00.000Z",
      "currency": "KES",
      "channel": "card"
    }
  },
  "message": "Payment details retrieved",
  "success": true
}
```

---

### 4. Callback Endpoint

**Called by Paystack:**
```bash
GET http://payment-service:8080/api/payments/callback?reference=ORDER-uuid-12345
```

**Response:**
```
Payment successful! Reference: ORDER-uuid-12345
```

---

### 5. Health Check

**Request:**
```bash
GET http://payment-service:8080/api/payments/health
```

**Response (200 OK):**
```json
{
  "data": "OK",
  "message": "Paystack payment service is running",
  "success": true
}
```

---

## Using from Other Services

### Method 1: WebClient (Recommended)

**Configuration (application.properties):**
```properties
payment.service.url=http://payment-service:8080
payment.service.connect-timeout=5000
payment.service.read-timeout=10000
```

**Controller:**
```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @PostMapping("/checkout")
    public Mono<PaymentResponse> checkout(@RequestBody CheckoutRequest request) {
        PaymentIntentRequest paymentRequest = PaymentIntentRequest.builder()
            .amount(request.getTotalAmount())
            .currency("KES")
            .email(request.getCustomerEmail())
            .description("Order: " + request.getOrderId())
            .build();
        
        return webClientBuilder
            .baseUrl("http://payment-service:8080")
            .build()
            .post()
            .uri("/api/payments/initialize")
            .bodyValue(paymentRequest)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> handlePaymentResponse(response));
    }
    
    @GetMapping("/{orderId}/verify-payment")
    public Mono<Boolean> verifyPayment(@PathVariable String orderId) {
        return webClientBuilder
            .baseUrl("http://payment-service:8080")
            .build()
            .get()
            .uri("/api/payments/verify?reference={ref}", orderId)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> (Boolean) response.getData())
            .onErrorReturn(false);
    }
}
```

### Method 2: Use PaymentServiceClient (If starter is a dependency)

**Configuration (pom.xml):**
```xml
<dependency>
    <groupId>com.paystack</groupId>
    <artifactId>paystack-payment-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Controller:**
```java
import com.paystack.payment.client.PaymentServiceClient;
import com.paystack.payment.dto.PaymentIntentRequest;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private PaymentServiceClient paymentClient;
    
    @PostMapping("/checkout")
    public Mono<PaymentResponse> checkout(@RequestBody CheckoutRequest request) {
        PaymentIntentRequest paymentRequest = PaymentIntentRequest.builder()
            .amount(request.getTotalAmount())
            .currency("KES")
            .email(request.getCustomerEmail())
            .description("Order: " + request.getOrderId())
            .build();
        
        return paymentClient.initializePayment(paymentRequest)
            .map(response -> handlePaymentResponse(response));
    }
    
    @GetMapping("/{orderId}/verify-payment")
    public Mono<Boolean> verifyPayment(@PathVariable String orderId) {
        return paymentClient.verifyPayment(orderId);
    }
}
```

---

## Complete Example: Order Service Integration

### 1. Order Service pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.9</version>
    </parent>

    <groupId>com.ecommerce</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-r2dbc</artifactId>
        </dependency>
        <!-- No paystack-payment-starter dependency needed! -->
    </dependencies>
</project>
```

### 2. Order Service application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: order-service
  r2dbc:
    url: r2dbc:postgresql://localhost/orderdb
    username: postgres
    password: password

payment:
  service:
    url: http://localhost:8080  # Payment service URL
    connect-timeout: 5000
    read-timeout: 10000
```

### 3. Order Service Controller

```java
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final WebClient.Builder webClientBuilder;
    @Value("${payment.service.url}")
    private String paymentServiceUrl;
    
    @PostMapping("/checkout")
    public Mono<OrderResponse> checkout(@RequestBody CheckoutRequest request) {
        return orderService.createOrder(request)
            .flatMap(order -> initializePayment(order))
            .flatMap(order -> Mono.just(orderToResponse(order)));
    }
    
    private Mono<Order> initializePayment(Order order) {
        PaymentIntentRequest paymentRequest = PaymentIntentRequest.builder()
            .amount(order.getTotalAmount())
            .currency("KES")
            .email(order.getCustomerEmail())
            .description("Order #" + order.getId())
            .callbackUrl("https://yourapi.com/orders/" + order.getId() + "/payment-callback")
            .build();
        
        return webClientBuilder
            .baseUrl(paymentServiceUrl)
            .build()
            .post()
            .uri("/api/payments/initialize")
            .bodyValue(paymentRequest)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> {
                if (response.isSuccess()) {
                    Map<String, Object> paymentData = (Map) response.getData();
                    order.setPaymentReference((String) paymentData.get("paymentIntentId"));
                    order.setAuthorizationUrl((String) paymentData.get("authorizationUrl"));
                    order.setPaymentStatus("INITIALIZED");
                    return order;
                } else {
                    throw new RuntimeException("Payment initialization failed");
                }
            });
    }
    
    @PostMapping("/{orderId}/payment-callback")
    public Mono<OrderResponse> paymentCallback(
        @PathVariable Long orderId,
        @RequestParam String reference
    ) {
        return webClientBuilder
            .baseUrl(paymentServiceUrl)
            .build()
            .get()
            .uri("/api/payments/verify?reference={ref}", reference)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .flatMap(response -> {
                if (response.isSuccess() && (Boolean) response.getData()) {
                    return orderService.updatePaymentStatus(orderId, "PAID");
                } else {
                    return orderService.updatePaymentStatus(orderId, "FAILED");
                }
            })
            .map(order -> orderToResponse(order));
    }
}
```

---

## HTTP Clients for Testing

### cURL

```bash
# Initialize payment
curl -X POST http://localhost:8080/api/payments/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "currency": "KES",
    "email": "test@example.com",
    "description": "Test payment"
  }'

# Verify payment
curl -X GET "http://localhost:8080/api/payments/verify?reference=ORDER-abc123"

# Health check
curl -X GET http://localhost:8080/api/payments/health
```

### Postman Collection

```json
{
  "info": {
    "name": "Paystack Payment Service",
    "version": "1.0"
  },
  "item": [
    {
      "name": "Initialize Payment",
      "request": {
        "method": "POST",
        "url": "{{payment_service}}/api/payments/initialize",
        "body": {
          "mode": "raw",
          "raw": "{\"amount\": 50000, \"currency\": \"KES\", \"email\": \"test@example.com\"}"
        }
      }
    },
    {
      "name": "Verify Payment",
      "request": {
        "method": "GET",
        "url": "{{payment_service}}/api/payments/verify?reference={{reference}}"
      }
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "{{payment_service}}/api/payments/health"
      }
    }
  ]
}
```

---

## Deployment Configuration

### Docker Compose

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
      - PAYSTACK_CALLBACK_URL=http://localhost:8000/payment_callback.php
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
      - payment-service
    environment:
      - PAYMENT_SERVICE_URL=http://payment-service:8080
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
      - name: payment-service
        image: paystack-payment-service:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: PAYSTACK_API_KEY
          valueFrom:
            secretKeyRef:
              name: paystack-secrets
              key: api-key
        livenessProbe:
          httpGet:
            path: /api/payments/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## Security Considerations

1. **Network Security**
   - Use HTTPS in production
   - Implement service-to-service authentication
   - Use VPN/private networks between services

2. **API Key Management**
   - Store Paystack API key in secrets manager
   - Never expose in logs or error messages
   - Rotate regularly

3. **Rate Limiting**
   - Implement per-service rate limits
   - Use API Gateway for centralized rate limiting

4. **Monitoring**
   - Log all payment operations
   - Set up alerts for failures
   - Monitor payment success rates

---

## Performance Tips

1. Use connection pooling (configured in WebClient)
2. Set appropriate timeouts
3. Implement caching for payment status
4. Use async/reactive endpoints
5. Monitor response times

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| 502 Bad Gateway | Check payment service is running |
| Connection timeout | Increase `read-timeout` and `connect-timeout` |
| 404 Payment not found | Verify reference is correct |
| Circuit breaker open | Wait for timeout or check Paystack API |

---

For detailed API reference, see the main README.md.

