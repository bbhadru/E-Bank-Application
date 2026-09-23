package com.ebank.controller;

import com.ebank.dto.LoanRepaymentRequest;
import com.ebank.dto.LoanRequest;
import com.ebank.model.Loan;
import com.ebank.model.LoanSchedule;
import com.ebank.model.LoanTransaction;
import com.ebank.service.LoanService;
import com.ebank.service.MemberService;
import com.ebank.service.SavingsAccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class LoanController {

    private final LoanService loanService;
    private final MemberService memberService;
    private final SavingsAccountService savingsAccountService;

    public LoanController(LoanService loanService, MemberService memberService, SavingsAccountService savingsAccountService) {
        this.loanService = loanService;
        this.memberService = memberService;
        this.savingsAccountService = savingsAccountService;
    }

    @GetMapping("/loans")
    public String listLoans(@RequestParam(value = "query", required = false) String query,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            Model model) {
        Page<Loan> loanPage = loanService.searchLoans(
                query, status, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("loans", loanPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", loanPage.getTotalPages());
        model.addAttribute("totalItems", loanPage.getTotalElements());
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        model.addAttribute("repaymentRequest", new LoanRepaymentRequest());
        return "loans/list";
    }

    @GetMapping("/loans/apply")
    public String applyLoanForm(Model model) {
        model.addAttribute("loanRequest", new LoanRequest());
        model.addAttribute("members", memberService.getAllMembersList(""));
        return "loans/apply";
    }

    @PostMapping("/loans/apply")
    public String applyLoan(@Valid @ModelAttribute("loanRequest") LoanRequest request,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("members", memberService.getAllMembersList(""));
            return "loans/apply";
        }
        Loan loan = loanService.applyLoan(request);
        redirectAttributes.addFlashAttribute("successMessage", "Loan application submitted! Loan No: " + loan.getLoanNumber());
        return "redirect:/loans";
    }

    @PostMapping("/loans/approve/{id}")
    public String approveLoan(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        loanService.approveLoan(id, 1L);
        redirectAttributes.addFlashAttribute("successMessage", "Loan approved successfully!");
        return "redirect:/loans";
    }

    @PostMapping("/loans/disburse/{id}")
    public String disburseLoan(@PathVariable("id") Long id,
                               @RequestParam(value = "targetSbAccountId", required = false) Long targetSbAccountId,
                               RedirectAttributes redirectAttributes) {
        loanService.disburseLoan(id, targetSbAccountId);
        redirectAttributes.addFlashAttribute("successMessage", "Loan disbursed successfully and amortization schedule generated!");
        return "redirect:/loans/schedule/" + id;
    }

    @PostMapping("/loans/repay")
    public String processRepayment(@Valid @ModelAttribute("repaymentRequest") LoanRepaymentRequest request,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid repayment details");
            return "redirect:/loans";
        }
        loanService.processRepayment(request);
        redirectAttributes.addFlashAttribute("successMessage", "Repayment of INR " + request.getAmount() + " processed!");
        return "redirect:/loans/schedule/" + request.getLoanId();
    }

    @GetMapping("/loans/schedule/{id}")
    public String viewSchedule(@PathVariable("id") Long id, Model model) {
        Loan loan = loanService.getLoanById(id);
        List<LoanSchedule> schedules = loanService.getAmortizationSchedule(id);
        List<LoanTransaction> transactions = loanService.getLoanTransactions(id);

        model.addAttribute("loan", loan);
        model.addAttribute("schedules", schedules);
        model.addAttribute("transactions", transactions);
        model.addAttribute("repaymentRequest", LoanRepaymentRequest.builder().loanId(id).amount(loan.getEmiAmount()).build());
        return "loans/schedule";
    }

    // REST APIs
    @GetMapping("/api/loans")
    @ResponseBody
    public ResponseEntity<List<Loan>> listLoansApi(@RequestParam(required = false) Long memberId) {
        if (memberId != null) {
            return ResponseEntity.ok(loanService.getLoansByMember(memberId));
        }
        return ResponseEntity.ok(loanService.searchLoans(null, null, PageRequest.of(0, 100)).getContent());
    }

    @PostMapping("/api/loans/apply")
    @ResponseBody
    public ResponseEntity<Loan> applyLoanApi(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.ok(loanService.applyLoan(request));
    }

    @PostMapping("/api/loans/{id}/approve")
    @ResponseBody
    public ResponseEntity<Loan> approveLoanApi(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.approveLoan(id, 1L));
    }

    @PostMapping("/api/loans/{id}/disburse")
    @ResponseBody
    public ResponseEntity<Loan> disburseLoanApi(@PathVariable Long id, @RequestParam(required = false) Long targetSbId) {
        return ResponseEntity.ok(loanService.disburseLoan(id, targetSbId));
    }

    @PostMapping("/api/loans/repay")
    @ResponseBody
    public ResponseEntity<LoanTransaction> repayLoanApi(@Valid @RequestBody LoanRepaymentRequest request) {
        return ResponseEntity.ok(loanService.processRepayment(request));
    }

    @GetMapping("/api/loans/{id}/schedule")
    @ResponseBody
    public ResponseEntity<List<LoanSchedule>> getScheduleApi(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getAmortizationSchedule(id));
    }
}
