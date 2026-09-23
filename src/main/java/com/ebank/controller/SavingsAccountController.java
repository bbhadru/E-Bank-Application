package com.ebank.controller;

import com.ebank.dto.SavingsAccountRequest;
import com.ebank.dto.TransactionRequest;
import com.ebank.model.Member;
import com.ebank.model.SavingsAccount;
import com.ebank.model.SbTransaction;
import com.ebank.service.MemberService;
import com.ebank.service.SavingsAccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
public class SavingsAccountController {

    private final SavingsAccountService savingsAccountService;
    private final MemberService memberService;

    public SavingsAccountController(SavingsAccountService savingsAccountService, MemberService memberService) {
        this.savingsAccountService = savingsAccountService;
        this.memberService = memberService;
    }

    @GetMapping("/accounts/savings")
    public String listAccounts(@RequestParam(value = "query", required = false) String query,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               Model model) {
        Page<SavingsAccount> accountPage = savingsAccountService.searchAccounts(
                query, status, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("accounts", accountPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", accountPage.getTotalPages());
        model.addAttribute("totalItems", accountPage.getTotalElements());
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        model.addAttribute("transactionRequest", new TransactionRequest());
        return "accounts/savings/list";
    }

    @GetMapping("/accounts/savings/create")
    public String createAccountForm(Model model) {
        model.addAttribute("savingsAccountRequest", new SavingsAccountRequest());
        model.addAttribute("members", memberService.getAllMembersList(""));
        return "accounts/savings/create";
    }

    @PostMapping("/accounts/savings/create")
    public String createAccount(@Valid @ModelAttribute("savingsAccountRequest") SavingsAccountRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("members", memberService.getAllMembersList(""));
            return "accounts/savings/create";
        }
        SavingsAccount created = savingsAccountService.createAccount(request);
        redirectAttributes.addFlashAttribute("successMessage", "Savings Account opened! Account No: " + created.getAccountNumber());
        return "redirect:/accounts/savings";
    }

    @GetMapping({"/accounts/savings/deposit", "/accounts/savings/withdraw"})
    public String redirectGetToSavingsList() {
        return "redirect:/accounts/savings";
    }

    @PostMapping("/accounts/savings/deposit")
    public String deposit(@Valid @ModelAttribute("transactionRequest") TransactionRequest request,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(org.springframework.validation.FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Invalid deposit details");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/accounts/savings";
        }
        if (request.getTxnType() == null || request.getTxnType().isBlank()) {
            request.setTxnType("DEPOSIT");
        }
        savingsAccountService.deposit(request);
        redirectAttributes.addFlashAttribute("successMessage", "Deposit of INR " + request.getAmount() + " successful!");
        return "redirect:/accounts/savings/statement/" + request.getAccountId();
    }

    @PostMapping("/accounts/savings/withdraw")
    public String withdraw(@Valid @ModelAttribute("transactionRequest") TransactionRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(org.springframework.validation.FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Invalid withdrawal details");
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/accounts/savings";
        }
        if (request.getTxnType() == null || request.getTxnType().isBlank()) {
            request.setTxnType("WITHDRAW");
        }
        savingsAccountService.withdraw(request);
        redirectAttributes.addFlashAttribute("successMessage", "Withdrawal of INR " + request.getAmount() + " successful!");
        return "redirect:/accounts/savings/statement/" + request.getAccountId();
    }

    @GetMapping("/accounts/savings/statement/{id}")
    public String statement(@PathVariable("id") Long id,
                            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            Model model) {
        SavingsAccount account = savingsAccountService.getAccountById(id);
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(LocalTime.MAX) : null;

        List<SbTransaction> transactions = savingsAccountService.getAccountStatement(id, fromDt, toDt);

        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("transactionRequest", new TransactionRequest());
        return "accounts/savings/statement";
    }

    // REST APIs
    @PostMapping("/api/accounts/savings")
    @ResponseBody
    public ResponseEntity<SavingsAccount> createAccountApi(@Valid @RequestBody SavingsAccountRequest request) {
        return ResponseEntity.ok(savingsAccountService.createAccount(request));
    }

    @PostMapping("/api/accounts/savings/deposit")
    @ResponseBody
    public ResponseEntity<SbTransaction> depositApi(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(savingsAccountService.deposit(request));
    }

    @PostMapping("/api/accounts/savings/withdraw")
    @ResponseBody
    public ResponseEntity<SbTransaction> withdrawApi(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(savingsAccountService.withdraw(request));
    }

    @GetMapping("/api/accounts/savings/{id}")
    @ResponseBody
    public ResponseEntity<SavingsAccount> getAccountApi(@PathVariable Long id) {
        return ResponseEntity.ok(savingsAccountService.getAccountById(id));
    }

    @GetMapping("/api/accounts/savings/{id}/statement")
    @ResponseBody
    public ResponseEntity<List<SbTransaction>> getStatementApi(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(LocalTime.MAX) : null;
        return ResponseEntity.ok(savingsAccountService.getAccountStatement(id, fromDt, toDt));
    }
}
