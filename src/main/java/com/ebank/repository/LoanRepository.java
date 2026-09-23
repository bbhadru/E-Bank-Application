package com.ebank.repository;

import com.ebank.model.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    List<Loan> findByMemberMemberId(Long memberId);

    List<Loan> findByStatus(String status);

    long countByStatus(String status);

    long countByNpaFlagTrue();

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'DISBURSED')")
    BigDecimal sumActiveOutstanding();

    @Query("SELECT l FROM Loan l WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(l.loanNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.member.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:status IS NULL OR :status = '' OR l.status = :status)")
    Page<Loan> searchLoans(@Param("query") String query, @Param("status") String status, Pageable pageable);

    @Query("SELECT MAX(l.loanId) FROM Loan l")
    Long findMaxId();
}
