# Paystack Payment Starter - Now with REST API! 🎉

## What's New

The paystack-payment-starter now includes **REST endpoints** that allow other backend services to call the payment service via HTTP using WebClient, **without needing to add the starter as a dependency**.

## Two Usage Modes

### Mode 1: Library (Original)
```
Your Service
    ↓
    (dependency)
    ↓
paystack-payment-starter library
    ↓ (uses)
    ↓
PaymentService bean
```
- Add as Maven dependency
- Use `PaymentService` directly
- Best for single applications

### Mode 2: Microservice (New!)
```
Service 1          Service 2          Service 3
    ↓                   ↓                  ↓
  WebClient → REST API ← WebClient    ← WebClient
                  ↓
    paystack-payment-starter
        (standalone service)
                  ↓
            PaymentService
                  ↓
            Paystack API
```
- Deploy as standalone microservice
- Call via HTTP REST API
- Other services don't need the dependency!
- Just need PaymentIntentRequest/Response DTOs

## What Was Added

### 1. PaymentController.java ✅
REST endpoints for payment operations:
- `POST /api/payments/initialize` - Initialize payment
- `GET /api/payments/verify?reference=...` - Verify payment
- `GET /api/payments/callback` - Handle Paystack callback
- `GET /api/payments/details?reference=...` - Get payment details
- `GET /api/payments/health` - Health check

### 2. ApiResponse.java ✅
Generic response wrapper for all REST endpoints.

### 3. PaymentServiceClient.java ✅
WebClient for calling the payment service from other services.

### 4. PaymentServiceClientProperties.java ✅
Configuration properties for payment service client.

### 5. REST_API_GUIDE.md ✅
Complete guide to using the REST API (200+ lines).

### 6. STANDALONE_SERVICE_GUIDE.md ✅
Step-by-step guide for standalone microservice deployment (300+ lines).

## Quick Examples

### Option 1: Call via Raw WebClient (No Dependency)

```java
@RestController
public class OrderController {
    
    @Autowired
    private WebClient.Builder webClientBuilder;
    
    @PostMapping("/checkout")
    public Mono<PaymentResponse> checkout(@RequestBody CheckoutRequest req) {
        PaymentIntentRequest paymentReq = PaymentIntentRequest.builder()
            .amount(req.getTotal())
            .currency("KES")
            .email(req.getEmail())
            .build();
        
        return webClientBuilder
            .baseUrl("http://payment-service:8080")
            .build()
            .post()
            .uri("/api/payments/initialize")
            .bodyValue(paymentReq)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> handleResponse(response));
    }
}
```

### Option 2: Use Provided PaymentServiceClient

```java
@Component
public class PaymentServiceClient {
    private final WebClient.Builder webClientBuilder;
    
    public Mono<PaymentIntentResponse> initializePayment(PaymentIntentRequest request) {
        return webClientBuilder
            .baseUrl("http://payment-service:8080")
            .build()
            .post()
            .uri("/api/payments/initialize")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(response -> response.getData());
    }
}
```

### Option 3: Add as Dependency (Original Way)

```java
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService;
    
    public Mono<PaymentResponse> initiate(PaymentIntentRequest req) {
        return paymentService.initializePayment(req)
            .map(paymentService::toPaymentIntentResponse);
    }
}
```

## REST API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/payments/initialize` | Initialize payment session |
| GET | `/api/payments/verify?reference=...` | Verify payment status |
| GET | `/api/payments/details?reference=...` | Get payment details |
| GET | `/api/payments/callback?reference=...` | Handle Paystack callback |
| GET | `/api/payments/health` | Check service health |

## Request/Response Examples

### Initialize Payment Request
```bash
POST http://payment-service:8080/api/payments/initialize
Content-Type: application/json

{
  "amount": 50000,
  "currency": "KES",
  "email": "customer@example.com",
  "description": "Order #123"
}
```

### Initialize Payment Response
```json
{
  "data": {
    "clientSecret": "AC_xxx",
    "paymentIntentId": "ORDER-uuid",
    "status": "initialized",
    "authorizationUrl": "https://checkout.paystack.com/..."
  },
  "message": "Payment initialized successfully",
  "success": true
}
```

### Verify Payment Request
```bash
GET http://payment-service:8080/api/payments/verify?reference=ORDER-uuid
```

### Verify Payment Response
```json
{
  "data": true,
  "message": "Payment verified successfully",
  "success": true
}
```

## Deployment Options

### Docker
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/paystack-payment-starter.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080
```

```bash
docker run -e PAYSTACK_API_KEY=sk_test_xxx -p 8080:8080 paystack-payment-service
```

### Docker Compose
```yaml
services:
  payment-service:
    build: ./paystack-payment-starter
    ports:
      - "8080:8080"
    environment:
      - PAYSTACK_API_KEY=sk_test_xxx
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 2
  template:
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
```

## How to Use

### For Standalone Microservice

**Step 1:** Create application.properties
```properties
server.port=8080
paystack.api.key=${PAYSTACK_API_KEY}
```

**Step 2:** Build
```bash
mvn clean package -f paystack-payment-starter/pom.xml
```

**Step 3:** Deploy
```bash
docker build -t paystack-payment-service .
docker run -e PAYSTACK_API_KEY=sk_test_xxx -p 8080:8080 paystack-payment-service
```

**Step 4:** Call from other services
```java
@Component
public class OrderService {
    private final WebClient webClient;
    
    public Mono<PaymentIntentResponse> pay(PaymentIntentRequest req) {
        return webClient.post()
            .uri("http://payment-service:8080/api/payments/initialize")
            .bodyValue(req)
            .retrieve()
            .bodyToMono(ApiResponse.class)
            .map(res -> res.getData());
    }
}
```

## Benefits

### Developers
- ✅ No dependency needed in consuming services
- ✅ Share only DTOs, not code
- ✅ Loosely coupled architecture
- ✅ Easy to test locally

### Operations
- ✅ Independent scaling of payment service
- ✅ Centralized Paystack configuration
- ✅ Easy monitoring and logging
- ✅ Single source of truth for payment logic

### Architecture
- ✅ Clean microservices design
- ✅ Reduced code duplication
- ✅ Clear service boundaries
- ✅ Easy to maintain

## Configuration

### Payment Service Configuration
```yaml
server:
  port: 8080

paystack:
  api:
    key: ${PAYSTACK_API_KEY}
    url: https://api.paystack.co
  callback:
    url: https://yourapi.com/payment-callback

resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        slidingWindowSize: 10
        failureRateThreshold: 50
```

### Consuming Service Configuration
```yaml
payment:
  service:
    url: http://payment-service:8080
    connect-timeout: 5000
    read-timeout: 10000
```

## Testing

### Local Setup
```bash
# Terminal 1: Start payment service
cd paystack-payment-starter
mvn spring-boot:run

# Terminal 2: Call the API
curl -X POST http://localhost:8080/api/payments/initialize \
  -H "Content-Type: application/json" \
  -d '{"amount": 50000, "currency": "KES", "email": "test@example.com"}'
```

### Health Check
```bash
curl http://localhost:8080/api/payments/health
```

### Response
```json
{
  "data": "OK",
  "message": "Paystack payment service is running",
  "success": true
}
```

## Files Added

- `PaymentController.java` - REST endpoints
- `ApiResponse.java` - Response wrapper
- `PaymentServiceClient.java` - WebClient for calling service
- `PaymentServiceClientProperties.java` - Configuration properties
- `REST_API_GUIDE.md` - REST API documentation
- `STANDALONE_SERVICE_GUIDE.md` - Standalone deployment guide

## Documentation

| Document | Purpose |
|----------|---------|
| `REST_API_GUIDE.md` | Complete REST API documentation |
| `STANDALONE_SERVICE_GUIDE.md` | Standalone microservice setup |
| `README.md` | Main documentation (updated) |

## Next Steps

1. **Try It Out**
   - Deploy payment service standalone
   - Call from another service via WebClient
   - See docs in `REST_API_GUIDE.md`

2. **Production Ready**
   - All error handling included
   - Health checks configured
   - Resilience patterns built-in
   - Logging enabled

3. **Scale**
   - Deploy multiple replicas
   - Use load balancer
   - Monitor performance

## Summary

The Paystack Payment Starter can now be used as:

1. **Library** - Add dependency, use PaymentService (original way)
2. **Microservice** - Deploy standalone, call via REST API (NEW!)

Choose based on your architecture:
- **Library** - Single application or tight coupling needed
- **Microservice** - Multiple services, want independence

Both approaches work great. Pick what fits your needs! 🚀

---

**Status: Production Ready** ✅

For detailed information, see:
- `REST_API_GUIDE.md` - REST API reference
- `STANDALONE_SERVICE_GUIDE.md` - Deployment guide
- `README.md` - Complete documentation

