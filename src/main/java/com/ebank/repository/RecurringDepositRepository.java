package com.ebank.repository;

import com.ebank.model.RecurringDeposit;
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
public interface RecurringDepositRepository extends JpaRepository<RecurringDeposit, Long> {

    Optional<RecurringDeposit> findByRdNumber(String rdNumber);

    List<RecurringDeposit> findByMemberMemberId(Long memberId);

    List<RecurringDeposit> findByStatus(String status);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(rd.monthlyAmount), 0) FROM RecurringDeposit rd WHERE rd.status = 'ACTIVE'")
    BigDecimal sumActiveMonthlyAmount();

    @Query("SELECT rd FROM RecurringDeposit rd WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(rd.rdNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(rd.member.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:status IS NULL OR :status = '' OR rd.status = :status)")
    Page<RecurringDeposit> searchDeposits(@Param("query") String query, @Param("status") String status, Pageable pageable);

    @Query("SELECT MAX(rd.rdId) FROM RecurringDeposit rd")
    Long findMaxId();
}
