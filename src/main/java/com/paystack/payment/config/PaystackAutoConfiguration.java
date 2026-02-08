package com.paystack.payment.config;

import com.paystack.payment.service.PaymentService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Spring Boot auto-configuration for Paystack payment starter.
 *
 * This configuration is automatically applied when the starter library is added to the classpath.
 * It registers the PaymentService bean and enables configuration properties.
 *
 * Configuration can be customized via application.properties or application.yml:
 * - paystack.api.key: Your Paystack API key
 * - paystack.api.url: Paystack API URL (default: https://api.paystack.co)
 * - paystack.callback.url: Default callback URL after payment
 */
@AutoConfiguration
@ConditionalOnClass(PaymentService.class)
@EnableConfigurationProperties({PaystackConfigProperties.class, PaymentServiceClientProperties.class})
public class PaystackAutoConfiguration {

    /**
     * Create PaymentService bean if it doesn't already exist.
     * This allows for easy overriding in tests or specific implementations.
     */
    @Bean
    @ConditionalOnMissingBean
    public PaymentService paymentService(PaystackConfigProperties config) {
        return new PaymentService(config);
    }

    /**
     * Create WebClient bean for internal use (calling Paystack API).
     * Used by PaymentService to communicate with Paystack.
     */
    @Bean
    @ConditionalOnMissingBean(name = "paystackWebClient")
    public WebClient paystackWebClient() {
        return WebClient.builder().build();
    }

    /**
     * Create WebClient bean for the payment service (other services calling this service).
     * Configured with timeouts and connection pooling.
     */
    @Bean
    @ConditionalOnMissingBean(name = "paymentServiceWebClient")
    public WebClient paymentServiceWebClient(PaymentServiceClientProperties properties) {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(properties.getReadTimeout()));

        return WebClient.builder()
            .baseUrl(properties.getUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}

