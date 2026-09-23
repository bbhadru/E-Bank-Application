package com.ebank.repository;

import com.ebank.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberCode(String memberCode);

    Optional<Member> findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    boolean existsByKycNumber(String kycNumber);

    long countByStatus(String status);

    @Query("SELECT m FROM Member m WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(m.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.memberCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "m.mobile LIKE CONCAT('%', :query, '%')) " +
           "AND (:status IS NULL OR :status = '' OR m.status = :status)")
    Page<Member> searchMembers(@Param("query") String query, @Param("status") String status, Pageable pageable);

    @Query("SELECT m FROM Member m WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(m.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.memberCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "m.mobile LIKE CONCAT('%', :query, '%'))")
    List<Member> searchMembersList(@Param("query") String query);

    @Query("SELECT MAX(m.memberId) FROM Member m")
    Long findMaxId();
}
