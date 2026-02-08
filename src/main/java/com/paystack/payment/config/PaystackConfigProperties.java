package com.paystack.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Paystack payment integration.
 * Properties are read from application.properties or application.yml with prefix "paystack".
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "paystack")
public class PaystackConfigProperties {
    private Api api = new Api();
    private Callback callback = new Callback();

    @Getter
    @Setter
    public static class Api {
        private String key; // Paystack API key
        private String url = "https://api.paystack.co"; // Paystack base URL
    }

    @Getter
    @Setter
    public static class Callback {
        private String url; // Default callback URL after payment
    }
}

