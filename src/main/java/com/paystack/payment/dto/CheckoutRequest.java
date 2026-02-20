package com.paystack.payment.dto;

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
public class CheckoutRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
}
