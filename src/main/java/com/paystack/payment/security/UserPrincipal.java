package com.paystack.payment.security;

import com.paystack.payment.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String email;
    private UserRole role;
}
