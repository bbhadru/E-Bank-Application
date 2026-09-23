package com.ebank.controller;

import com.ebank.dto.VoucherRequest;
import com.ebank.model.LedgerAccount;
import com.ebank.model.Voucher;
import com.ebank.service.VoucherService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping("/vouchers")
    public String listVouchers(@RequestParam(value = "query", required = false) String query,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               Model model) {
        Page<Voucher> voucherPage = voucherService.searchVouchers(
                query, type, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("vouchers", voucherPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", voucherPage.getTotalPages());
        model.addAttribute("totalItems", voucherPage.getTotalElements());
        model.addAttribute("query", query);
        model.addAttribute("type", type);
        return "vouchers/list";
    }

    @GetMapping("/vouchers/create")
    public String createVoucherForm(Model model) {
        List<LedgerAccount> accounts = voucherService.getAllLedgerAccounts();
        VoucherRequest request = new VoucherRequest();
        request.setVoucherDate(LocalDate.now());

        // Default 2 entries for double-entry
        List<VoucherRequest.LedgerEntryRequest> entries = new ArrayList<>();
        entries.add(new VoucherRequest.LedgerEntryRequest());
        entries.add(new VoucherRequest.LedgerEntryRequest());
        request.setEntries(entries);

        model.addAttribute("voucherRequest", request);
        model.addAttribute("accounts", accounts);
        return "vouchers/create";
    }

    @PostMapping("/vouchers/create")
    public String createVoucher(@Valid @ModelAttribute("voucherRequest") VoucherRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("accounts", voucherService.getAllLedgerAccounts());
            return "vouchers/create";
        }
        Voucher v = voucherService.createVoucher(request);
        redirectAttributes.addFlashAttribute("successMessage", "Voucher posted successfully! Voucher No: " + v.getVoucherNumber());
        return "redirect:/vouchers";
    }

    // REST APIs
    @GetMapping("/api/vouchers")
    @ResponseBody
    public ResponseEntity<List<Voucher>> listVouchersApi() {
        return ResponseEntity.ok(voucherService.searchVouchers(null, null, PageRequest.of(0, 100)).getContent());
    }

    @PostMapping("/api/vouchers")
    @ResponseBody
    public ResponseEntity<Voucher> createVoucherApi(@Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.createVoucher(request));
    }

    @GetMapping("/api/vouchers/accounts")
    @ResponseBody
    public ResponseEntity<List<LedgerAccount>> getAccountsApi() {
        return ResponseEntity.ok(voucherService.getAllLedgerAccounts());
    }
}
