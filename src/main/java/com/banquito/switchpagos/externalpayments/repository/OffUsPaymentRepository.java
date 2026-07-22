package com.banquito.switchpagos.externalpayments.repository;

import com.banquito.switchpagos.externalpayments.model.OffUsPayment;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OffUsPaymentRepository extends JpaRepository<OffUsPayment, UUID> {
    Optional<OffUsPayment> findByLineId(UUID lineId);
    Optional<OffUsPayment> findByIdempotencyKey(String idempotencyKey);
    List<OffUsPayment> findByBatchIdOrderBySequenceNumberAsc(UUID batchId);
    List<OffUsPayment> findByStatusInAndNextStatusQueryAtLessThanEqual(Collection<String> statuses, OffsetDateTime now);
}
