package com.banquito.switchpagos.externalpayments.client;

import com.banquito.switchpagos.externalpayments.dto.mock.CoreAccountingRequest;
import com.banquito.switchpagos.externalpayments.dto.mock.CoreAccountingResponse;

public interface OffUsCoreAccountingClient {
    CoreAccountingResponse registerProcessedOffUs(CoreAccountingRequest request);
}
