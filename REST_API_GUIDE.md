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

For detailed API reference, see the main README.md.

