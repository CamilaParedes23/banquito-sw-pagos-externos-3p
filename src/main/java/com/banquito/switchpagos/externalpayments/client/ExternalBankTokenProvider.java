package com.banquito.switchpagos.externalpayments.client;

import com.banquito.switchpagos.externalpayments.dto.interbank.TokenResponse;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "external.bank.client.mode", havingValue = "real")
public class ExternalBankTokenProvider {
    private static final long EXPIRY_SKEW_SECONDS = 60;

    private final RestClient externalBankRestClient;
    private final String tokenPath;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public ExternalBankTokenProvider(
            @Qualifier("externalBankRestClient") RestClient externalBankRestClient,
            @Value("${external.bank.token-path}") String tokenPath,
            @Value("${external.bank.client-id}") String clientId,
            @Value("${external.bank.client-secret}") String clientSecret) {
        this.externalBankRestClient = externalBankRestClient;
        this.tokenPath = tokenPath;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getBearerToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(EXPIRY_SKEW_SECONDS))) {
            return cachedToken;
        }
        if (isBlank(clientId) || isBlank(clientSecret)) {
            throw new ExternalBankClientException(
                    "EXTERNAL_BANK_CREDENTIALS_MISSING",
                    "No estan configuradas las credenciales OAuth2 del banco externo.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        try {
            TokenResponse response = externalBankRestClient.post()
                    .uri(tokenPath)
                    .headers(headers -> applyBasicAuth(headers))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || isBlank(response.accessToken())) {
                throw new ExternalBankClientException("EXTERNAL_BANK_TOKEN_EMPTY", "El banco externo no devolvio access_token.");
            }
            cachedToken = response.accessToken();
            long expiresIn = response.expiresIn() == null ? 3600 : response.expiresIn();
            expiresAt = Instant.now().plusSeconds(expiresIn);
            return cachedToken;
        } catch (RestClientResponseException exception) {
            throw new ExternalBankClientException(
                    "EXTERNAL_BANK_TOKEN_REJECTED",
                    "El banco externo rechazo la autenticacion OAuth2. httpStatus=" + exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            throw new ExternalBankTimeoutException("Timeout o error de conectividad al obtener token OAuth2 del banco externo.");
        }
    }

    private void applyBasicAuth(HttpHeaders headers) {
        headers.setBasicAuth(clientId, clientSecret);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
