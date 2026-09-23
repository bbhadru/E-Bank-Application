package com.ebank.repository;

import com.ebank.model.RdTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RdTransactionRepository extends JpaRepository<RdTransaction, Long> {

    List<RdTransaction> findByRecurringDepositRdIdOrderByInstallmentNoAsc(Long rdId);

    @Query("SELECT t FROM RdTransaction t WHERE t.status = 'PENDING' AND t.dueDate <= :date")
    List<RdTransaction> findDueInstallments(@Param("date") LocalDate date);

    @Query("SELECT COUNT(t) FROM RdTransaction t WHERE t.status = 'PENDING' AND t.dueDate < :date")
    long countOverdueInstallments(@Param("date") LocalDate date);
}
