package com.ebank.controller;

import com.ebank.dto.MemberRequest;
import com.ebank.dto.MemberResponse;
import com.ebank.model.Member;
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
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/members")
    public String listMembers(@RequestParam(value = "query", required = false) String query,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "10") int size,
                              Model model) {
        Page<MemberResponse> memberPage = memberService.searchMembers(
                query, status, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("members", memberPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", memberPage.getTotalPages());
        model.addAttribute("totalItems", memberPage.getTotalElements());
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        return "members/list";
    }

    @GetMapping("/members/add")
    public String addMemberForm(Model model) {
        model.addAttribute("memberRequest", new MemberRequest());
        return "members/add";
    }

    @PostMapping("/members/add")
    public String createMember(@Valid @ModelAttribute("memberRequest") MemberRequest memberRequest,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "members/add";
        }
        Member created = memberService.createMember(memberRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Member created successfully! Code: " + created.getMemberCode());
        return "redirect:/members";
    }

    @GetMapping("/members/edit/{id}")
    public String editMemberForm(@PathVariable("id") Long id, Model model) {
        Member member = memberService.getMemberById(id);
        MemberRequest request = MemberRequest.builder()
                .fullName(member.getFullName())
                .dob(member.getDob())
                .gender(member.getGender())
                .mobile(member.getMobile())
                .email(member.getEmail())
                .address(member.getAddress())
                .kycNumber(member.getKycNumber())
                .build();
        model.addAttribute("memberRequest", request);
        model.addAttribute("memberId", id);
        model.addAttribute("memberCode", member.getMemberCode());
        return "members/edit";
    }

    @PostMapping("/members/edit/{id}")
    public String updateMember(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("memberRequest") MemberRequest memberRequest,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("memberId", id);
            return "members/edit";
        }
        memberService.updateMember(id, memberRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Member updated successfully!");
        return "redirect:/members";
    }

    @PostMapping("/members/delete/{id}")
    public String deleteMember(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        memberService.deleteMember(id);
        redirectAttributes.addFlashAttribute("successMessage", "Member deactivated successfully!");
        return "redirect:/members";
    }

    // REST APIs
    @GetMapping("/api/members")
    @ResponseBody
    public ResponseEntity<List<MemberResponse>> listMembersApi(@RequestParam(value = "query", required = false) String query) {
        return ResponseEntity.ok(memberService.getAllMembersList(query));
    }

    @PostMapping("/api/members")
    @ResponseBody
    public ResponseEntity<MemberResponse> createMemberApi(@Valid @RequestBody MemberRequest request) {
        Member created = memberService.createMember(request);
        return ResponseEntity.ok(memberService.mapToResponse(created));
    }

    @GetMapping("/api/members/{id}")
    @ResponseBody
    public ResponseEntity<MemberResponse> getMemberApi(@PathVariable Long id) {
        Member member = memberService.getMemberById(id);
        return ResponseEntity.ok(memberService.mapToResponse(member));
    }

    @PutMapping("/api/members/{id}")
    @ResponseBody
    public ResponseEntity<MemberResponse> updateMemberApi(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        Member updated = memberService.updateMember(id, request);
        return ResponseEntity.ok(memberService.mapToResponse(updated));
    }

    @DeleteMapping("/api/members/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteMemberApi(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok().build();
    }
}
