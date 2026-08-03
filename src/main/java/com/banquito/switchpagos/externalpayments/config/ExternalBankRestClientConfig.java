package com.banquito.switchpagos.externalpayments.config;

import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "external.bank.client.mode", havingValue = "real")
public class ExternalBankRestClientConfig {

    @Bean
    public RestClient externalBankRestClient(
            @Value("${external.bank.base-url}") String baseUrl,
            @Value("${external.bank.connect-timeout-ms}") Integer connectTimeoutMs,
            @Value("${external.bank.read-timeout-ms}") Integer readTimeoutMs,
            @Value("${external.bank.insecure-ssl-enabled}") Boolean insecureSslEnabled) {
        if (Boolean.TRUE.equals(insecureSslEnabled)) {
            return RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(insecureSslRequestFactory(connectTimeoutMs, readTimeoutMs))
                    .build();
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private JdkClientHttpRequestFactory insecureSslRequestFactory(Integer connectTimeoutMs, Integer readTimeoutMs) {
        try {
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());

            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm(null);

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                    .sslContext(sslContext)
                    .sslParameters(sslParameters)
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
            return requestFactory;
        } catch (NoSuchAlgorithmException | KeyManagementException exception) {
            throw new IllegalStateException("No fue posible configurar SSL inseguro para banco externo.", exception);
        }
    }
}
