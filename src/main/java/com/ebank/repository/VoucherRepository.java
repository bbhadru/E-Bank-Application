package com.ebank.repository;

import com.ebank.model.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByVoucherNumber(String voucherNumber);

    List<Voucher> findByVoucherDateBetweenOrderByVoucherDateDesc(LocalDate from, LocalDate to);

    @Query("SELECT v FROM Voucher v WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(v.voucherNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(v.narration) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:voucherType IS NULL OR :voucherType = '' OR v.voucherType = :voucherType)")
    Page<Voucher> searchVouchers(@Param("query") String query, @Param("voucherType") String voucherType, Pageable pageable);

    @Query("SELECT MAX(v.voucherId) FROM Voucher v")
    Long findMaxId();
}
