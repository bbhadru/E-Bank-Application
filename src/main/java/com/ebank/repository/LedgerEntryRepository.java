package com.ebank.repository;

import com.ebank.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByVoucherVoucherId(Long voucherId);

    List<LedgerEntry> findByLedgerAccountLedgerIdOrderByCreatedAtAsc(Long ledgerId);

    List<LedgerEntry> findByLedgerAccountLedgerIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long ledgerId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(e.debit), 0) FROM LedgerEntry e WHERE e.ledgerAccount.ledgerId = :ledgerId")
    BigDecimal sumDebitByLedgerId(@Param("ledgerId") Long ledgerId);

    @Query("SELECT COALESCE(SUM(e.credit), 0) FROM LedgerEntry e WHERE e.ledgerAccount.ledgerId = :ledgerId")
    BigDecimal sumCreditByLedgerId(@Param("ledgerId") Long ledgerId);
}
