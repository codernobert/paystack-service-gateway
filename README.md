# Paystack Payment Spring Boot Starter

A reusable Spring Boot starter library for easy Paystack payment integration in microservices. This library encapsulates Paystack API communication, provides reactive programming support via Spring WebFlux, and includes resilience patterns (circuit breaker, retry, time limiter).

## Features

- ✅ **Easy Integration**: Zero-config auto-configuration; just add the dependency
- ✅ **Reactive Support**: Built with Spring WebFlux and Project Reactor for non-blocking operations
- ✅ **Multi-currency**: Supports any Paystack-supported currency (NGN, KES, USD, etc.)
- ✅ **Resilience**: Includes circuit breaker, retry, and time limiter patterns
- ✅ **Clean API**: Public DTOs hide internal Paystack implementation details
- ✅ **Well-documented**: Clear examples and configuration templates included

## Requirements

- Java 17+
- Spring Boot 3.5.9+
- Spring WebFlux (for reactive operations)

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.paystack</groupId>
    <artifactId>paystack-payment-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```gradle
implementation 'com.paystack:paystack-payment-starter:1.0.0'
```

## Configuration

### 1. Add Properties to `application.properties` or `application.yml`

**application.properties:**
```properties
# Paystack API Configuration
paystack.api.key=sk_live_xxxxxxxxxxxxxxxxxxxx
paystack.api.url=https://api.paystack.co
paystack.callback.url=https://yourapp.com/payment-callback

# Resilience4j Circuit Breaker
resilience4j.circuitbreaker.instances.paymentService.slidingWindowSize=10
resilience4j.circuitbreaker.instances.paymentService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.paymentService.waitDurationInOpenState=10s

# Resilience4j Retry
resilience4j.retry.instances.paymentService.maxAttempts=3
resilience4j.retry.instances.paymentService.waitDuration=1000ms

# Resilience4j Time Limiter
resilience4j.timelimiter.instances.paymentService.timeoutDuration=5s
```

**application.yml:**
```yaml
paystack:
  api:
    key: sk_live_xxxxxxxxxxxxxxxxxxxx
    url: https://api.paystack.co
  callback:
    url: https://yourapp.com/payment-callback

resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  retry:
    instances:
      paymentService:
        maxAttempts: 3
        waitDuration: 1000ms
  timelimiter:
    instances:
      paymentService:
        timeoutDuration: 5s
```

### 2. Environment Variables (Optional)

You can also use environment variables for sensitive configuration:

```bash
export PAYSTACK_API_KEY=sk_live_xxxxxxxxxxxxxxxxxxxx
```

Or in `.env` file (with Spring Cloud Config or similar):
```
PAYSTACK_API_KEY=sk_live_xxxxxxxxxxxxxxxxxxxx
```

## Usage

The starter can be used in two ways:

### Option 1: Library Dependency (Traditional)
Add as a Maven dependency to your Spring Boot application and inject PaymentService directly.
- **Best for**: Single application or tightly coupled microservices
- **Setup**: 5 minutes
- **See:** [Usage](#usage-as-library) section below

### Option 2: Standalone Microservice (Recommended for Microservices)
Deploy as a separate microservice and call via REST API.
- **Best for**: Microservices architecture, multiple independent services
- **Setup**: 10 minutes
- **No dependency needed** in other services!
- **See:** [REST_API_GUIDE.md](REST_API_GUIDE.md) and [STANDALONE_SERVICE_GUIDE.md](STANDALONE_SERVICE_GUIDE.md)

```java
import com.paystack.payment.service.PaymentService;
import com.paystack.payment.dto.PaymentIntentRequest;
import com.paystack.payment.dto.PaymentIntentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create")
    public Mono<PaymentIntentResponse> createPayment(@RequestBody PaymentIntentRequest request) {
        // PaymentService is auto-wired; just use it!
        return paymentService.initializePayment(request)
            .map(paymentService::toPaymentIntentResponse);
    }

    @GetMapping("/verify")
    public Mono<Boolean> verifyPayment(@RequestParam String reference) {
        return paymentService.verifyPaymentStatus(reference);
    }
}
```

### Create Payment Request DTO

```java
PaymentIntentRequest request = PaymentIntentRequest.builder()
    .amount(50000)                           // 50,000 kobo = 500 NGN
    .currency("NGN")                         // Nigerian Naira
    .email("customer@example.com")
    .description("Purchase Order #12345")
    .callbackUrl("https://yourapp.com/payment-callback")  // Optional
    .build();

Mono<PaymentIntentResponse> response = paymentService.initializePayment(request)
    .map(paymentService::toPaymentIntentResponse);
```

### Verify Payment Status

```java
paymentService.verifyPaymentStatus("ORDER-uuid-here")
    .subscribe(
        isSuccessful -> {
            if (isSuccessful) {
                System.out.println("Payment successful!");
            } else {
                System.out.println("Payment failed!");
            }
        },
        error -> System.out.println("Error verifying payment: " + error.getMessage())
    );
```

### Full Example: Payment Controller

```java
import com.paystack.payment.dto.PaymentIntentRequest;
import com.paystack.payment.dto.PaymentIntentResponse;
import com.paystack.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CheckoutController {
    
    private final PaymentService paymentService;

    @PostMapping("/payment-intent")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PaymentIntentResponse> createPaymentIntent(
        @Valid @RequestBody PaymentIntentRequest request
    ) {
        return paymentService.initializePayment(request)
            .map(paymentService::toPaymentIntentResponse);
    }

    @PostMapping("/verify-payment")
    public Mono<Boolean> verifyPayment(@RequestParam String reference) {
        return paymentService.verifyPaymentStatus(reference);
    }

    @GetMapping("/paystack/callback")
    public Mono<String> paystackCallback(@RequestParam String reference) {
        // Called by Paystack after payment
        return paymentService.verifyPaymentStatus(reference)
            .map(success -> success ?
                "redirect:/payment-success?reference=" + reference :
                "redirect:/payment-failed?reference=" + reference);
    }
}
```

## API Reference

### PaymentService Methods

#### 1. `initializePayment(PaymentIntentRequest request)`
Initializes a Paystack payment session.

**Parameters:**
- `request`: PaymentIntentRequest object with amount, currency, email, etc.

**Returns:** `Mono<PaystackInitializeResponse>`

**Example:**
```java
PaymentIntentRequest req = PaymentIntentRequest.builder()
    .amount(100000)
    .currency("KES")
    .email("user@example.com")
    .build();

Mono<PaystackInitializeResponse> response = paymentService.initializePayment(req);
```

#### 2. `verifyPayment(String reference)`
Verifies a payment transaction by reference.

**Parameters:**
- `reference`: Transaction reference returned from initialization

**Returns:** `Mono<PaystackVerifyResponse>`

**Example:**
```java
Mono<PaystackVerifyResponse> response = paymentService.verifyPayment("ORDER-12345");
```

#### 3. `verifyPaymentStatus(String reference)`
Convenience method to check if payment was successful.

**Parameters:**
- `reference`: Transaction reference

**Returns:** `Mono<Boolean>` (true if successful, false otherwise)

**Example:**
```java
Mono<Boolean> isSuccessful = paymentService.verifyPaymentStatus("ORDER-12345");
```

#### 4. `toPaymentIntentResponse(PaystackInitializeResponse paystackResponse)`
Converts internal Paystack response to public DTO.

**Parameters:**
- `paystackResponse`: Internal Paystack response

**Returns:** `PaymentIntentResponse`

**Example:**
```java
PaymentIntentResponse publicResponse = paymentService.toPaymentIntentResponse(paystackResponse);
```

## DTOs

### PaymentIntentRequest (Public API)
Used by consuming services to request payments.

```java
@Data
public class PaymentIntentRequest {
    private Long amount;           // In smallest currency unit (kobo, cents, etc.)
    private String currency;       // e.g., "NGN", "KES", "USD"
    private String description;    // Optional
    private String email;          // Required by Paystack
    private String callbackUrl;    // Optional; uses configured URL if not provided
}
```

### PaymentIntentResponse (Public API)
Returned after successful payment initialization.

```java
@Data
public class PaymentIntentResponse {
    private String clientSecret;      // Paystack access code
    private String paymentIntentId;   // Transaction reference
    private String status;            // "initialized"
    private String authorizationUrl;  // URL to redirect user for payment
}
```

## Error Handling

The PaymentService includes resilience patterns to handle failures gracefully:

### Circuit Breaker
- Opens after 50% failure rate
- Transitions to half-open after 10 seconds
- Allows 3 calls in half-open state

### Retry
- Retries up to 3 times
- Waits 1 second between attempts

### Time Limiter
- Timeouts after 5 seconds

### Custom Error Handling

```java
paymentService.initializePayment(request)
    .map(paymentService::toPaymentIntentResponse)
    .onErrorResume(error -> {
        log.error("Payment initialization failed: {}", error.getMessage());
        return Mono.error(new PaymentException("Could not initialize payment"));
    })
    .subscribe(
        response -> System.out.println("Payment initialized: " + response.getAuthorizationUrl()),
        error -> System.out.println("Error: " + error.getMessage())
    );
```

## Testing

### Mock PaymentService in Tests

```java
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.mockito.Mockito.*;

@TestConfiguration
public class TestPaymentConfig {
    
    @Bean
    @Primary
    public PaymentService mockPaymentService() {
        return mock(PaymentService.class);
    }
}

@SpringBootTest
@Import(TestPaymentConfig.class)
class PaymentControllerTest {
    
    @MockBean
    private PaymentService paymentService;
    
    @Test
    void testPaymentInitialization() {
        // Setup mock
        PaymentIntentResponse mockResponse = PaymentIntentResponse.builder()
            .authorizationUrl("https://checkout.paystack.com/...")
            .paymentIntentId("ORDER-123")
            .build();
            
        when(paymentService.initializePayment(any()))
            .thenReturn(Mono.just(mockResponse));
        
        // Test
        // ...
    }
}
```

## Support for Multi-Currency

The starter supports all Paystack-supported currencies. When creating a payment request, specify the currency:

```java
// Nigerian Naira
PaymentIntentRequest ngnRequest = PaymentIntentRequest.builder()
    .amount(50000)     // 50,000 kobo = 500 NGN
    .currency("NGN")
    .email("user@example.com")
    .build();

// Kenya Shillings (with M-Pesa support)
PaymentIntentRequest kesRequest = PaymentIntentRequest.builder()
    .amount(50000)     // 50,000 cents = 500 KES
    .currency("KES")
    .email("user@example.com")
    .build();

// US Dollars
PaymentIntentRequest usdRequest = PaymentIntentRequest.builder()
    .amount(5000)      // 5,000 cents = 50 USD
    .currency("USD")
    .email("user@example.com")
    .build();
```

## Troubleshooting

### "Paystack API key is not configured"
- Ensure `paystack.api.key` is set in `application.properties` or via `PAYSTACK_API_KEY` environment variable
- Get your API key from [Paystack Dashboard](https://dashboard.paystack.com/#/settings/developers)

### "Payment service temporarily unavailable"
- The circuit breaker is open due to repeated failures
- Check your internet connection and Paystack API status
- Wait for the circuit breaker to transition to half-open state (default: 10 seconds)

### "Connection timeout"
- Increase `resilience4j.timelimiter.instances.paymentService.timeoutDuration`
- Check if Paystack API is reachable from your network

## Version History

### 1.0.0 (2025-02-08)
- Initial release
- Payment initialization
- Payment verification
- Circuit breaker, retry, and time limiter patterns
- Multi-currency support

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Support

For issues, questions, or suggestions, please open an issue on GitHub.

## Additional Resources

- [Paystack API Documentation](https://paystack.com/docs/api/)
- [Spring WebFlux Guide](https://spring.io/guides/gs/reactive-rest-service/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Boot Auto-Configuration](https://spring.io/guides/gs/spring-boot/)

---

**Happy integrating! 🚀**

