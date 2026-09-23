package com.ebank.repository;

import com.ebank.model.Member;
import com.ebank.model.SavingsAccount;
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
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    Optional<SavingsAccount> findByAccountNumber(String accountNumber);

    List<SavingsAccount> findByMember(Member member);

    List<SavingsAccount> findByMemberMemberId(Long memberId);

    List<SavingsAccount> findByStatus(String status);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(s.balance), 0) FROM SavingsAccount s WHERE s.status = 'ACTIVE'")
    BigDecimal sumActiveBalances();

    @Query("SELECT s FROM SavingsAccount s WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(s.accountNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.member.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "s.member.memberCode LIKE CONCAT('%', :query, '%')) " +
           "AND (:status IS NULL OR :status = '' OR s.status = :status)")
    Page<SavingsAccount> searchAccounts(@Param("query") String query, @Param("status") String status, Pageable pageable);

    @Query("SELECT MAX(s.sbId) FROM SavingsAccount s")
    Long findMaxId();
}
