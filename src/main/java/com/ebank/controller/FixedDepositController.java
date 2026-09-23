package com.ebank.controller;

import com.ebank.dto.FixedDepositRequest;
import com.ebank.model.FixedDeposit;
import com.ebank.service.FixedDepositService;
import com.ebank.service.MemberService;
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
public class FixedDepositController {

    private final FixedDepositService fixedDepositService;
    private final MemberService memberService;

    public FixedDepositController(FixedDepositService fixedDepositService, MemberService memberService) {
        this.fixedDepositService = fixedDepositService;
        this.memberService = memberService;
    }

    @GetMapping("/accounts/fd")
    public String listDeposits(@RequestParam(value = "query", required = false) String query,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               Model model) {
        Page<FixedDeposit> fdPage = fixedDepositService.searchDeposits(
                query, status, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("deposits", fdPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", fdPage.getTotalPages());
        model.addAttribute("totalItems", fdPage.getTotalElements());
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        return "accounts/fd/list";
    }

    @GetMapping("/accounts/fd/create")
    public String createDepositForm(Model model) {
        model.addAttribute("fixedDepositRequest", new FixedDepositRequest());
        model.addAttribute("members", memberService.getAllMembersList(""));
        return "accounts/fd/create";
    }

    @PostMapping("/accounts/fd/create")
    public String createDeposit(@Valid @ModelAttribute("fixedDepositRequest") FixedDepositRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("members", memberService.getAllMembersList(""));
            return "accounts/fd/create";
        }
        FixedDeposit fd = fixedDepositService.createFixedDeposit(request);
        redirectAttributes.addFlashAttribute("successMessage", "Fixed Deposit created! FD No: " + fd.getFdNumber());
        return "redirect:/accounts/fd";
    }

    @PostMapping("/accounts/fd/close/{id}")
    public String closeDeposit(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        fixedDepositService.matureOrCloseDeposit(id);
        redirectAttributes.addFlashAttribute("successMessage", "Fixed Deposit closed / matured successfully!");
        return "redirect:/accounts/fd";
    }

    // REST APIs
    @GetMapping("/api/accounts/fd")
    @ResponseBody
    public ResponseEntity<List<FixedDeposit>> listDepositsApi(@RequestParam(required = false) Long memberId) {
        if (memberId != null) {
            return ResponseEntity.ok(fixedDepositService.getDepositsByMember(memberId));
        }
        return ResponseEntity.ok(fixedDepositService.searchDeposits(null, null, PageRequest.of(0, 100)).getContent());
    }

    @PostMapping("/api/accounts/fd")
    @ResponseBody
    public ResponseEntity<FixedDeposit> createDepositApi(@Valid @RequestBody FixedDepositRequest request) {
        return ResponseEntity.ok(fixedDepositService.createFixedDeposit(request));
    }

    @PostMapping("/api/accounts/fd/{id}/close")
    @ResponseBody
    public ResponseEntity<?> closeDepositApi(@PathVariable Long id) {
        fixedDepositService.matureOrCloseDeposit(id);
        return ResponseEntity.ok().build();
    }
}
