package com.ebank.controller;

import com.ebank.dto.ShareCapitalRequest;
import com.ebank.model.ShareCapital;
import com.ebank.service.MemberService;
import com.ebank.service.ShareCapitalService;
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
public class ShareCapitalController {

    private final ShareCapitalService shareCapitalService;
    private final MemberService memberService;

    public ShareCapitalController(ShareCapitalService shareCapitalService, MemberService memberService) {
        this.shareCapitalService = shareCapitalService;
        this.memberService = memberService;
    }

    @GetMapping("/shares")
    public String listShares(@RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "10") int size,
                             Model model) {
        Page<ShareCapital> sharePage = shareCapitalService.searchShares(
                query, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("shares", sharePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sharePage.getTotalPages());
        model.addAttribute("totalItems", sharePage.getTotalElements());
        model.addAttribute("query", query);
        model.addAttribute("shareRequest", new ShareCapitalRequest());
        model.addAttribute("members", memberService.getAllMembersList(""));
        return "shares/list";
    }

    @PostMapping("/shares/allot")
    public String allotShares(@Valid @ModelAttribute("shareRequest") ShareCapitalRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid share allotment data");
            return "redirect:/shares";
        }
        ShareCapital sc = shareCapitalService.allotShares(request);
        redirectAttributes.addFlashAttribute("successMessage", "Shares allotted! Certificate No: " + sc.getCertificateNumber());
        return "redirect:/shares";
    }

    @PostMapping("/shares/dividend")
    public String distributeDividend(@RequestParam("dividendPerShare") BigDecimal dividendPerShare,
                                     RedirectAttributes redirectAttributes) {
        shareCapitalService.distributeDividend(dividendPerShare);
        redirectAttributes.addFlashAttribute("successMessage", "Dividend of INR " + dividendPerShare + " per share distributed successfully!");
        return "redirect:/shares";
    }

    // REST APIs
    @GetMapping("/api/shares")
    @ResponseBody
    public ResponseEntity<List<ShareCapital>> listSharesApi(@RequestParam(required = false) Long memberId) {
        if (memberId != null) {
            return ResponseEntity.ok(shareCapitalService.getSharesByMember(memberId));
        }
        return ResponseEntity.ok(shareCapitalService.searchShares(null, PageRequest.of(0, 100)).getContent());
    }

    @PostMapping("/api/shares/allot")
    @ResponseBody
    public ResponseEntity<ShareCapital> allotSharesApi(@Valid @RequestBody ShareCapitalRequest request) {
        return ResponseEntity.ok(shareCapitalService.allotShares(request));
    }

    @PostMapping("/api/shares/dividend")
    @ResponseBody
    public ResponseEntity<?> distributeDividendApi(@RequestParam BigDecimal dividendPerShare) {
        shareCapitalService.distributeDividend(dividendPerShare);
        return ResponseEntity.ok().build();
    }
}
