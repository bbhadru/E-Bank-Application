package com.ebank.repository;

import com.ebank.model.LoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, Long> {

    List<LoanTransaction> findByLoanLoanIdOrderByPaymentDateDesc(Long loanId);

    @Query("SELECT COALESCE(SUM(lt.amount), 0) FROM LoanTransaction lt WHERE lt.paymentDate BETWEEN :from AND :to")
    BigDecimal sumAmountByPaymentDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
