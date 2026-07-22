package com.banquito.switchpagos.externalpayments.client;

import com.banquito.switchpagos.externalpayments.dto.mock.CoreAccountingRequest;
import com.banquito.switchpagos.externalpayments.dto.mock.CoreAccountingResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockOffUsCoreAccountingClient implements OffUsCoreAccountingClient {

    private final Boolean defaultSuccess;
    private final Map<String, CoreAccountingResponse> responses = new ConcurrentHashMap<>();

    public MockOffUsCoreAccountingClient(@Value("${offus.core.mock.default-success}") Boolean defaultSuccess) {
        this.defaultSuccess = defaultSuccess;
    }

    @Override
    public CoreAccountingResponse registerProcessedOffUs(CoreAccountingRequest request) {
        CoreAccountingResponse existing = responses.get(request.idempotencyKey());
        if (existing != null) {
            return existing;
        }
        boolean shouldFail = contains(request.reference(), "MOCK_CORE_FAIL")
                || contains(request.reference(), "CORE_ACCOUNTING_FAILED")
                || Boolean.FALSE.equals(defaultSuccess);
        CoreAccountingResponse response = shouldFail
                ? new CoreAccountingResponse(false, null, "CORE_ACCOUNTING_FAILED", "Registro contable Off-Us simulado fallido.")
                : new CoreAccountingResponse(true, "MOCK-CORE-OFFUS-" + request.lineId().toString().substring(0, 8).toUpperCase(), "00", "Registro contable Off-Us simulado.");
        responses.put(request.idempotencyKey(), response);
        return response;
    }

    private static boolean contains(String value, String token) {
        return value != null && value.toUpperCase().contains(token);
    }
}
