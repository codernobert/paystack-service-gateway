package com.paystack.payment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class PaystackWebClientConfig {

    @Value("${paystack.api.url}")
    private String paystackApiUrl;

    @Value("${paystack.api.key}")
    private String paystackApiKey;

    @Bean
    public WebClient paystackWebClient() {

        return WebClient.builder()
                .baseUrl(paystackApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + paystackApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * Log outgoing requests
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Paystack Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    /**
     * Log responses
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Paystack Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}