package com.ebank.controller;

import com.ebank.dto.RecurringDepositRequest;
import com.ebank.model.RdTransaction;
import com.ebank.model.RecurringDeposit;
import com.ebank.service.MemberService;
import com.ebank.service.RecurringDepositService;
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

import java.math.BigDecimal;
import java.util.List;

@Controller
public class RecurringDepositController {

    private final RecurringDepositService recurringDepositService;
    private final MemberService memberService;

    public RecurringDepositController(RecurringDepositService recurringDepositService, MemberService memberService) {
        this.recurringDepositService = recurringDepositService;
        this.memberService = memberService;
    }

    @GetMapping("/accounts/rd")
    public String listDeposits(@RequestParam(value = "query", required = false) String query,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               Model model) {
        Page<RecurringDeposit> rdPage = recurringDepositService.searchDeposits(
                query, status, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("deposits", rdPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", rdPage.getTotalPages());
        model.addAttribute("totalItems", rdPage.getTotalElements());
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        return "accounts/rd/list";
    }

    @GetMapping("/accounts/rd/create")
    public String createDepositForm(Model model) {
        model.addAttribute("recurringDepositRequest", new RecurringDepositRequest());
        model.addAttribute("members", memberService.getAllMembersList(""));
        return "accounts/rd/create";
    }

    @PostMapping("/accounts/rd/create")
    public String createDeposit(@Valid @ModelAttribute("recurringDepositRequest") RecurringDepositRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("members", memberService.getAllMembersList(""));
            return "accounts/rd/create";
        }
        RecurringDeposit rd = recurringDepositService.createRecurringDeposit(request);
        redirectAttributes.addFlashAttribute("successMessage", "Recurring Deposit created! RD No: " + rd.getRdNumber());
        return "redirect:/accounts/rd";
    }

    @GetMapping("/accounts/rd/{id}")
    public String viewDeposit(@PathVariable("id") Long id, Model model) {
        RecurringDeposit rd = recurringDepositService.getDepositById(id);
        List<RdTransaction> installments = recurringDepositService.getInstallments(id);
        model.addAttribute("deposit", rd);
        model.addAttribute("installments", installments);
        return "accounts/rd/view";
    }

    @PostMapping("/accounts/rd/pay-installment")
    public String payInstallment(@RequestParam("rdId") Long rdId,
                                 @RequestParam("installmentNo") Integer installmentNo,
                                 @RequestParam(value = "penalty", required = false) BigDecimal penalty,
                                 RedirectAttributes redirectAttributes) {
        recurringDepositService.payInstallment(rdId, installmentNo, penalty);
        redirectAttributes.addFlashAttribute("successMessage", "Installment #" + installmentNo + " paid successfully!");
        return "redirect:/accounts/rd";
    }

    @PostMapping("/accounts/rd/close/{id}")
    public String closeDeposit(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        recurringDepositService.closeOrMatureDeposit(id);
        redirectAttributes.addFlashAttribute("successMessage", "Recurring Deposit closed successfully!");
        return "redirect:/accounts/rd";
    }

    // REST APIs
    @GetMapping("/api/accounts/rd")
    @ResponseBody
    public ResponseEntity<List<RecurringDeposit>> listDepositsApi(@RequestParam(required = false) Long memberId) {
        if (memberId != null) {
            return ResponseEntity.ok(recurringDepositService.getDepositsByMember(memberId));
        }
        return ResponseEntity.ok(recurringDepositService.searchDeposits(null, null, PageRequest.of(0, 100)).getContent());
    }

    @PostMapping("/api/accounts/rd")
    @ResponseBody
    public ResponseEntity<RecurringDeposit> createDepositApi(@Valid @RequestBody RecurringDepositRequest request) {
        return ResponseEntity.ok(recurringDepositService.createRecurringDeposit(request));
    }

    @PostMapping("/api/accounts/rd/{id}/pay")
    @ResponseBody
    public ResponseEntity<RdTransaction> payInstallmentApi(@PathVariable Long id,
                                                           @RequestParam Integer installmentNo,
                                                           @RequestParam(required = false) BigDecimal penalty) {
        return ResponseEntity.ok(recurringDepositService.payInstallment(id, installmentNo, penalty));
    }
}
