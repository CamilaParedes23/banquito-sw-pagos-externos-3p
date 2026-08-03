package com.banquito.switchpagos.externalpayments.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "external.bank.client.mode", havingValue = "real")
public class ExternalBankRestClientConfig {

    @Bean
    public RestClient externalBankRestClient(
            @Value("${external.bank.base-url}") String baseUrl,
            @Value("${external.bank.connect-timeout-ms}") Integer connectTimeoutMs,
            @Value("${external.bank.read-timeout-ms}") Integer readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
