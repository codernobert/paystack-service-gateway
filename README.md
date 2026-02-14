# Paystack Payment Service

A reusable Spring Boot service for easy Paystack payment integration in microservices. This service encapsulates Paystack API communication, provides reactive programming support via Spring WebFlux, and includes resilience patterns (circuit breaker, retry, time limiter).

## Features

- ✅ **Easy Integration**: Standalone microservice with REST API endpoints
- ✅ **Reactive Support**: Built with Spring WebFlux and Project Reactor for non-blocking operations
- ✅ **Multi-currency**: Supports any Paystack-supported currency (NGN, KES, USD, etc.)
- ✅ **Resilience**: Includes circuit breaker, retry, and time limiter patterns
- ✅ **Clean API**: RESTful endpoints for payment operations
- ✅ **Well-documented**: Clear examples and configuration templates included
- ✅ **No Dependencies Required**: Other services call via REST API without adding dependencies

## Requirements

- Java 17+
- Spring Boot 3.5.9+
- Spring WebFlux (for reactive operations)
- Docker (optional, for containerized deployment)

## Installation & Deployment

### Option 1: Local Development

1. Clone the repository:
```bash
git clone <repository-url>
cd paystack-payment-service
```

2. Configure your environment variables or `application.properties`:
```bash
export PAYSTACK_API_KEY=sk_live_xxxxxxxxxxxxxxxxxxxx
```

3. Run the service:
```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

### Option 2: Docker Deployment

Build and run using Docker:
```bash
# Build the Docker image
docker build -t paystack-payment-service:1.0.0 .

# Run the container
docker run -e PAYSTACK_API_KEY=sk_live_xxxx \
           -e PAYSTACK_CALLBACK_URL=https://webhook.site/... \
           -p 8080:8080 \
           paystack-payment-service:1.0.0
```

## Configuration

### 1. Environment Variables (Recommended for Production)

Set these environment variables before starting the service:

```bash
# Required
PAYSTACK_API_KEY=sk_live_xxxxxxxxxxxxxxxxxxxx

# Optional (defaults provided)
PAYSTACK_API_URL=https://api.paystack.co
PAYSTACK_CALLBACK_URL=https://yourapp.com/payment-callback
```

### 2. Application Properties (Development)

Edit `application.properties` or `application.yml`:

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


Or in `.env` file (with Spring Cloud Config or similar):
```
PAYSTACK_API_KEY=sk_live_xxxxxxxxxxxxxxxxxxxx
```

## Usage

This is a **standalone microservice**. Other services call it via REST API endpoints.

### REST API Endpoints

The service exposes the following endpoints:

#### 1. Initialize Payment
```http
POST /api/payments/initialize
Content-Type: application/json

{
  "amount": 50000,
  "currency": "KES",
  "email": "customer@example.com",
  "description": "Order #12345",
  "callbackUrl": "https://yourapp.com/payment-callback"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Payment initialized successfully",
  "data": {
    "paymentIntentId": "ORDER-a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
    "clientSecret": "AC_test_xxxxxxxxxxxxxxxxxxxx",
    "status": "initialized",
    "authorizationUrl": "https://checkout.paystack.com/...",
    "accessCode": "access_code_xxx",
    "reference": "ref_xxx"
  }
}
```

#### 2. Verify Payment
```http
GET /api/payments/verify?reference=ORDER-a1b2c3d4
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payment verified successfully",
  "data": true
}
```

#### 3. Get Payment Details
```http
GET /api/payments/details?reference=ORDER-a1b2c3d4
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payment details retrieved",
  "data": {
    "id": 123456789,
    "status": "success",
    "amount": 50000,
    "currency": "KES",
    "customer": {
      "email": "customer@example.com",
      "first_name": "John",
      "last_name": "Doe"
    }
  }
}
```

#### 4. Payment Callback (Called by Paystack)
```http
GET /api/payments/callback?reference=ref_xxx
```

#### 5. Health Check
```http
GET /api/payments/health
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Paystack payment service is running",
  "data": "OK"
}
```

### Example: Calling from Another Service (Node.js)

```javascript
// Initialize Payment
const initResponse = await fetch('http://paystack-service:8080/api/payments/initialize', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    amount: 50000,
    currency: 'KES',
    email: 'customer@example.com',
    description: 'Order #12345'
  })
});

const { data } = await initResponse.json();
const authUrl = data.authorizationUrl;
const reference = data.paymentIntentId;

// Redirect user to payment page
window.location.href = authUrl;

// After payment, verify
const verifyResponse = await fetch(
  `http://paystack-service:8080/api/payments/verify?reference=${reference}`
);
const verification = await verifyResponse.json();
console.log('Payment successful:', verification.data);
```

### Example: Calling from Another Service (Java/Spring)

```java
import org.springframework.web.reactive.function.client.WebClient;
import com.paystack.payment.dto.PaymentIntentRequest;
import reactor.core.publisher.Mono;

@Component
public class OrderService {
    
    private final WebClient webClient;
    
    public OrderService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://paystack-service:8080")
            .build();
    }
    
    public Mono<String> createPayment(Long amount, String email) {
        PaymentIntentRequest request = PaymentIntentRequest.builder()
            .amount(amount)
            .currency("KES")
            .email(email)
            .description("Order payment")
            .build();
        
        return webClient.post()
            .uri("/api/payments/initialize")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return (String) data.get("authorizationUrl");
            });
    }
    
    public Mono<Boolean> verifyPayment(String reference) {
        return webClient.get()
            .uri("/api/payments/verify?reference={reference}", reference)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> (Boolean) response.get("data"));
    }
}
```

### Example: Calling from Another Service (Python)

```python
import requests

# Initialize payment
response = requests.post(
    'http://paystack-service:8080/api/payments/initialize',
    json={
        'amount': 50000,
        'currency': 'KES',
        'email': 'customer@example.com',
        'description': 'Order #12345'
    }
)

data = response.json()['data']
auth_url = data['authorizationUrl']
reference = data['paymentIntentId']

# Verify payment
verify_response = requests.get(
    f'http://paystack-service:8080/api/payments/verify?reference={reference}'
)
is_successful = verify_response.json()['data']
print(f"Payment successful: {is_successful}")
```

### Request/Response Models

#### PaymentIntentRequest

```json
{
  "amount": 50000,           // Required: Amount in smallest currency unit
  "currency": "KES",         // Required: Currency code
  "email": "user@example.com", // Required: Customer email
  "description": "Order details", // Optional: Payment description
  "callbackUrl": "https://..." // Optional: Custom callback URL
}
```

#### PaymentIntentResponse

```json
{
  "paymentIntentId": "ORDER-uuid",
  "clientSecret": "AC_test_xxxx",
  "status": "initialized",
  "authorizationUrl": "https://checkout.paystack.com/...",
  "accessCode": "access_code_xxx",
  "reference": "ref_xxx"
}
```

## API Reference

### REST API Endpoints

#### 1. POST /api/payments/initialize
Initializes a Paystack payment session.

**Request:**
```bash
curl -X POST http://localhost:8080/api/payments/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "currency": "KES",
    "email": "customer@example.com",
    "description": "Order #12345"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Payment initialized successfully",
  "data": {
    "paymentIntentId": "ORDER-uuid",
    "clientSecret": "AC_test_xxxx",
    "status": "initialized",
    "authorizationUrl": "https://checkout.paystack.com/...",
    "accessCode": "access_code_xxx",
    "reference": "ref_xxx"
  }
}
```

#### 2. GET /api/payments/verify
Verifies a payment transaction by reference.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/payments/verify?reference=ORDER-uuid"
```

**Response:**
```json
{
  "success": true,
  "message": "Payment verified successfully",
  "data": true
}
```

#### 3. GET /api/payments/details
Get detailed payment information from Paystack.

**Request:**
```bash
curl -X GET "http://localhost:8080/api/payments/details?reference=ORDER-uuid"
```

**Response:**
```json
{
  "success": true,
  "message": "Payment details retrieved",
  "data": {
    "id": 123456789,
    "status": "success",
    "amount": 50000,
    "currency": "KES",
    "customer": {
      "email": "customer@example.com",
      "first_name": "John",
      "last_name": "Doe"
    }
  }
}
```

#### 4. GET /api/payments/callback
Handle Paystack callback (called by Paystack after payment).

**Request:**
```bash
curl -X GET "http://localhost:8080/api/payments/callback?reference=ref_xxx"
```

#### 5. GET /api/payments/health
Health check endpoint.

**Request:**
```bash
curl -X GET http://localhost:8080/api/payments/health
```

**Response:**
```json
{
  "success": true,
  "message": "Paystack payment service is running",
  "data": "OK"
}
```

## Internal Service Reference

For developers working on the service itself, here are the key internal methods:

### PaymentService Methods

#### 1. `initializePayment(PaymentIntentRequest request)`
Initializes a Paystack payment session internally.

**Returns:** `Mono<PaystackInitializeResponse>`

#### 2. `verifyPayment(String reference)`
Verifies a payment transaction internally.

**Returns:** `Mono<PaystackVerifyResponse>`

#### 3. `verifyPaymentStatus(String reference)`
Checks if payment was successful internally.

**Returns:** `Mono<Boolean>`

#### 4. `toPaymentIntentResponse(PaystackInitializeResponse paystackResponse)`
Converts internal Paystack response to public DTO.

**Returns:** `PaymentIntentResponse`


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

## Testing

### Using Postman

A comprehensive Postman collection is provided in `Postman_Collection.json` for testing all endpoints.

**To import:**
1. Open Postman
2. Click `File` → `Import`
3. Select `Postman_Collection.json`
4. Start testing the endpoints

**Test Flow:**
1. Health Check - verify service is running
2. Initialize Payment - create a new payment
3. Verify Payment - check payment status
4. Get Payment Details - retrieve full payment info
5. Callback - simulate Paystack callback

### Testing with cURL

```bash
# Health check
curl -X GET http://localhost:8080/api/payments/health

# Initialize payment
curl -X POST http://localhost:8080/api/payments/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "currency": "KES",
    "email": "test@example.com",
    "description": "Test payment"
  }'

# Verify payment (replace REFERENCE with actual reference)
curl -X GET "http://localhost:8080/api/payments/verify?reference=REFERENCE"

# Get details (replace REFERENCE with actual reference)
curl -X GET "http://localhost:8080/api/payments/details?reference=REFERENCE"
```

### Testing with Docker

```bash
# Build image
docker build -t paystack-payment-service:1.0.0 .

# Run with test configuration
docker run \
  -e PAYSTACK_API_KEY=sk_test_95764a2e73d66e1fe944ea4c0deba3386f70f46f \
  -e PAYSTACK_CALLBACK_URL=https://webhook.site/0a43d12b-660c-4fb3-84ef-9b7b9b367eb0 \
  -p 8080:8080 \
  paystack-payment-service:1.0.0

# Test health endpoint
curl http://localhost:8080/api/payments/health
```

## Support for Multi-Currency

The service supports all Paystack-supported currencies. When creating a payment request via the API, specify the currency:

```bash
# Nigerian Naira
curl -X POST http://localhost:8080/api/payments/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "currency": "NGN",
    "email": "user@example.com",
    "description": "Payment in NGN"
  }'

# Kenya Shillings (with M-Pesa support)
curl -X POST http://localhost:8080/api/payments/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "currency": "KES",
    "email": "user@example.com",
    "description": "Payment in KES"
  }'

# US Dollars
curl -X POST http://localhost:8080/api/payments/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000,
    "currency": "USD",
    "email": "user@example.com",
    "description": "Payment in USD"
  }'
```

## Troubleshooting

### Service won't start
- Ensure `PAYSTACK_API_KEY` environment variable is set
- Check that port 8080 is not in use
- Verify Java 17+ is installed

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

### Cannot connect to service from another container
- Ensure both containers are on the same Docker network
- Use container hostname instead of `localhost` (e.g., `http://paystack-service:8080`)
- Check firewall rules if running on a separate machine

## Version History

### 1.0.0 (2026-02-08)
- Initial release as standalone microservice
- REST API endpoints for payment operations
- Payment initialization, verification, and details retrieval
- Circuit breaker, retry, and time limiter patterns
- Multi-currency support (NGN, KES, USD, etc.)
- Docker containerization support
- Comprehensive Postman collection for testing
- Webhook support for Paystack callbacks

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

For issues, questions, or suggestions, please open an issue on GitHub or reach me at: norbertokoth01@gmail.com

For real-time payment testing, use the webhook endpoint: https://webhook.site/0a43d12b-660c-4fb3-84ef-9b7b9b367eb0

## Additional Resources

- [Paystack API Documentation](https://paystack.com/docs/api/)
- [Spring WebFlux Guide](https://spring.io/guides/gs/reactive-rest-service/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Boot Reference Guide](https://spring.io/projects/spring-boot)
- [Docker Documentation](https://docs.docker.com/)

---

**Happy integrating! 🚀**

