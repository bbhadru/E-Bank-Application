package com.ebank.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AccountNumberGenerator {

    private final AtomicLong memberSequence = new AtomicLong(1000);
    private final AtomicLong sbSequence = new AtomicLong(100000);
    private final AtomicLong fdSequence = new AtomicLong(200000);
    private final AtomicLong rdSequence = new AtomicLong(300000);
    private final AtomicLong loanSequence = new AtomicLong(400000);
    private final AtomicLong shareSequence = new AtomicLong(500000);
    private final AtomicLong voucherSequence = new AtomicLong(1);

    public String generateMemberCode(Long nextId) {
        long id = nextId != null ? nextId : memberSequence.incrementAndGet();
        return String.format("MEM-%05d", id);
    }

    public String generateSbAccountNumber(Long nextId) {
        long id = nextId != null ? (10000000L + nextId) : sbSequence.incrementAndGet();
        return String.format("SB%08d", id);
    }

    public String generateFdNumber(Long nextId) {
        long id = nextId != null ? (20000000L + nextId) : fdSequence.incrementAndGet();
        return String.format("FD%08d", id);
    }

    public String generateRdNumber(Long nextId) {
        long id = nextId != null ? (30000000L + nextId) : rdSequence.incrementAndGet();
        return String.format("RD%08d", id);
    }

    public String generateLoanNumber(Long nextId) {
        long id = nextId != null ? (40000000L + nextId) : loanSequence.incrementAndGet();
        return String.format("LN%08d", id);
    }

    public String generateCertificateNumber(Long nextId) {
        long id = nextId != null ? (50000000L + nextId) : shareSequence.incrementAndGet();
        return String.format("SH%08d", id);
    }

    public String generateVoucherNumber(Long nextId) {
        int year = LocalDate.now().getYear();
        long id = nextId != null ? nextId : voucherSequence.incrementAndGet();
        return String.format("VCH-%d-%05d", year, id);
    }
}
