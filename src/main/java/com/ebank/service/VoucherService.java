package com.ebank.service;

import com.ebank.dto.VoucherRequest;
import com.ebank.exception.InvalidOperationException;
import com.ebank.exception.ResourceNotFoundException;
import com.ebank.model.LedgerAccount;
import com.ebank.model.LedgerEntry;
import com.ebank.model.Voucher;
import com.ebank.repository.LedgerAccountRepository;
import com.ebank.repository.LedgerEntryRepository;
import com.ebank.repository.VoucherRepository;
import com.ebank.util.AccountNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AuditService auditService;

    public VoucherService(VoucherRepository voucherRepository,
                          LedgerAccountRepository ledgerAccountRepository,
                          LedgerEntryRepository ledgerEntryRepository,
                          AccountNumberGenerator accountNumberGenerator,
                          AuditService auditService) {
        this.voucherRepository = voucherRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.auditService = auditService;
    }

    @Transactional
    public Voucher createVoucher(VoucherRequest request) {
        // Validate double-entry
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (VoucherRequest.LedgerEntryRequest entryReq : request.getEntries()) {
            if (entryReq.getDebit() != null) totalDebit = totalDebit.add(entryReq.getDebit());
            if (entryReq.getCredit() != null) totalCredit = totalCredit.add(entryReq.getCredit());
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new InvalidOperationException("Double-entry violation: Total Debits (" + totalDebit +
                    ") must equal Total Credits (" + totalCredit + ")");
        }

        Long maxId = voucherRepository.findMaxId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        String voucherNumber = accountNumberGenerator.generateVoucherNumber(nextId);

        Voucher voucher = Voucher.builder()
                .voucherNumber(voucherNumber)
                .voucherType(request.getVoucherType())
                .voucherDate(request.getVoucherDate() != null ? request.getVoucherDate() : LocalDate.now())
                .narration(request.getNarration())
                .amount(request.getAmount())
                .status("POSTED")
                .createdAt(LocalDateTime.now())
                .build();

        Voucher savedVoucher = voucherRepository.save(voucher);

        for (VoucherRequest.LedgerEntryRequest entryReq : request.getEntries()) {
            LedgerAccount account = ledgerAccountRepository.findById(entryReq.getLedgerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ledger account not found with id: " + entryReq.getLedgerId()));

            BigDecimal debit = entryReq.getDebit() != null ? entryReq.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = entryReq.getCredit() != null ? entryReq.getCredit() : BigDecimal.ZERO;

            LedgerEntry entry = LedgerEntry.builder()
                    .voucher(savedVoucher)
                    .ledgerAccount(account)
                    .debit(debit)
                    .credit(credit)
                    .narration(entryReq.getNarration() != null ? entryReq.getNarration() : request.getNarration())
                    .createdAt(LocalDateTime.now())
                    .build();

            ledgerEntryRepository.save(entry);

            // Update Ledger Account current balance
            updateAccountBalance(account, debit, credit);
        }

        auditService.log(null, "VOUCHER_POSTED", "Voucher", savedVoucher.getVoucherId(), null, voucherNumber);
        return savedVoucher;
    }

    private void updateAccountBalance(LedgerAccount account, BigDecimal debit, BigDecimal credit) {
        String type = account.getLedgerType();
        BigDecimal balance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;

        if ("ASSET".equalsIgnoreCase(type) || "EXPENSE".equalsIgnoreCase(type)) {
            balance = balance.add(debit).subtract(credit);
        } else { // LIABILITY or INCOME
            balance = balance.add(credit).subtract(debit);
        }

        account.setCurrentBalance(balance);
        ledgerAccountRepository.save(account);
    }

    @Transactional
    public Voucher postAutomatedVoucher(String voucherType, String narration,
                                       String debitLedgerCode, BigDecimal debitAmount,
                                       String creditLedgerCode, BigDecimal creditAmount) {
        LedgerAccount debitAccount = getOrCreateLedgerAccount(debitLedgerCode);
        LedgerAccount creditAccount = getOrCreateLedgerAccount(creditLedgerCode);

        List<VoucherRequest.LedgerEntryRequest> entries = new ArrayList<>();
        entries.add(VoucherRequest.LedgerEntryRequest.builder()
                .ledgerId(debitAccount.getLedgerId())
                .debit(debitAmount)
                .credit(BigDecimal.ZERO)
                .narration(narration)
                .build());

        entries.add(VoucherRequest.LedgerEntryRequest.builder()
                .ledgerId(creditAccount.getLedgerId())
                .debit(BigDecimal.ZERO)
                .credit(creditAmount)
                .narration(narration)
                .build());

        VoucherRequest request = VoucherRequest.builder()
                .voucherType(voucherType)
                .voucherDate(LocalDate.now())
                .narration(narration)
                .amount(debitAmount)
                .entries(entries)
                .build();

        return createVoucher(request);
    }

    @Transactional
    public LedgerAccount getOrCreateLedgerAccount(String code) {
        return ledgerAccountRepository.findByLedgerCode(code)
                .orElseGet(() -> {
                    String name = code.replace("_", " ");
                    String type = "ASSET";
                    if (code.contains("DEPOSIT") || code.contains("CAPITAL") || code.contains("PAYABLE")) {
                        type = "LIABILITY";
                    } else if (code.contains("INCOME") || code.contains("INTEREST_REC")) {
                        type = "INCOME";
                    } else if (code.contains("EXPENSE") || code.contains("INTEREST_PAID")) {
                        type = "EXPENSE";
                    }

                    LedgerAccount acc = LedgerAccount.builder()
                            .ledgerCode(code)
                            .ledgerName(name)
                            .ledgerType(type)
                            .openingBalance(BigDecimal.ZERO)
                            .currentBalance(BigDecimal.ZERO)
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return ledgerAccountRepository.save(acc);
                });
    }

    @Transactional(readOnly = true)
    public Page<Voucher> searchVouchers(String query, String voucherType, Pageable pageable) {
        return voucherRepository.searchVouchers(query, voucherType, pageable);
    }

    @Transactional(readOnly = true)
    public List<LedgerAccount> getAllLedgerAccounts() {
        return ledgerAccountRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Voucher getVoucherById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));
    }
}
