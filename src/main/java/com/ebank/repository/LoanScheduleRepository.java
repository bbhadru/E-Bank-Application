package com.ebank.repository;

import com.ebank.model.LoanSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {

    List<LoanSchedule> findByLoanLoanIdOrderByInstallmentNoAsc(Long loanId);

    List<LoanSchedule> findByLoanLoanIdAndPaidStatusFalseOrderByInstallmentNoAsc(Long loanId);

    @Query("SELECT s FROM LoanSchedule s WHERE s.paidStatus = false AND s.dueDate < :date")
    List<LoanSchedule> findOverdueSchedules(@Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM LoanSchedule s WHERE s.paidStatus = false AND s.dueDate < :date")
    long countOverdueSchedules(@Param("date") LocalDate date);
}
