package com.ebank.repository;

import com.ebank.model.SbTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SbTransactionRepository extends JpaRepository<SbTransaction, Long> {

    List<SbTransaction> findBySavingsAccountSbIdOrderByTxnDateDesc(Long sbId);

    Page<SbTransaction> findBySavingsAccountSbIdOrderByTxnDateDesc(Long sbId, Pageable pageable);

    List<SbTransaction> findBySavingsAccountSbIdAndTxnDateBetweenOrderByTxnDateAsc(
            Long sbId, LocalDateTime startDate, LocalDateTime endDate);

    long countByTxnDateBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM SbTransaction t WHERE t.txnDate BETWEEN :from AND :to")
    BigDecimal sumAmountByTxnDateBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM SbTransaction t WHERE t.txnType = 'DEPOSIT' AND t.txnDate BETWEEN :from AND :to")
    BigDecimal sumDepositsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM SbTransaction t WHERE t.txnType = 'WITHDRAW' AND t.txnDate BETWEEN :from AND :to")
    BigDecimal sumWithdrawalsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
