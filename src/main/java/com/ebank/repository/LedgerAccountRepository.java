package com.ebank.repository;

import com.ebank.model.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    Optional<LedgerAccount> findByLedgerCode(String ledgerCode);

    Optional<LedgerAccount> findByLedgerName(String ledgerName);

    List<LedgerAccount> findByLedgerType(String ledgerType);

    List<LedgerAccount> findByIsActiveTrue();
}
