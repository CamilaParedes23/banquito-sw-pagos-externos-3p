package com.banquito.switchpagos.externalpayments.repository;

import com.banquito.switchpagos.externalpayments.model.OffUsPaymentAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OffUsPaymentAttemptRepository extends JpaRepository<OffUsPaymentAttempt, UUID> {
    Long countByExternalPaymentOperationId(UUID externalPaymentOperationId);
    List<OffUsPaymentAttempt> findByExternalPaymentOperationIdOrderByAttemptNumberAsc(UUID externalPaymentOperationId);
}
