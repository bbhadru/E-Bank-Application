package com.ebank.repository;

import com.ebank.model.ShareCapital;
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
public interface ShareCapitalRepository extends JpaRepository<ShareCapital, Long> {

    List<ShareCapital> findByMemberMemberId(Long memberId);

    Optional<ShareCapital> findByCertificateNumber(String certificateNumber);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM ShareCapital s WHERE s.status = 'ACTIVE'")
    BigDecimal sumTotalCapital();

    @Query("SELECT s FROM ShareCapital s WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(s.certificateNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.member.fullName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<ShareCapital> searchShares(@Param("query") String query, Pageable pageable);

    @Query("SELECT MAX(s.shareId) FROM ShareCapital s")
    Long findMaxId();
}
