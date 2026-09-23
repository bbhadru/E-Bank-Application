package com.ebank.repository;

import com.ebank.model.FixedDeposit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {

    Optional<FixedDeposit> findByFdNumber(String fdNumber);

    List<FixedDeposit> findByMemberMemberId(Long memberId);

    List<FixedDeposit> findByStatus(String status);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(fd.principal), 0) FROM FixedDeposit fd WHERE fd.status = 'ACTIVE'")
    BigDecimal sumActivePrincipal();

    @Query("SELECT fd FROM FixedDeposit fd WHERE fd.status = 'ACTIVE' AND fd.maturityDate <= :date")
    List<FixedDeposit> findMaturingDeposits(@Param("date") LocalDate date);

    @Query("SELECT COUNT(fd) FROM FixedDeposit fd WHERE fd.status = 'ACTIVE' AND fd.maturityDate <= :date")
    long countMaturingDeposits(@Param("date") LocalDate date);

    @Query("SELECT fd FROM FixedDeposit fd WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(fd.fdNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(fd.member.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:status IS NULL OR :status = '' OR fd.status = :status)")
    Page<FixedDeposit> searchDeposits(@Param("query") String query, @Param("status") String status, Pageable pageable);

    @Query("SELECT MAX(fd.fdId) FROM FixedDeposit fd")
    Long findMaxId();
}
