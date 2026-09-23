package com.ebank.controller;

import com.ebank.dto.ReportFilter;
import com.ebank.service.ReportService;
import com.ebank.util.ReportExporter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public String reportsIndex(@RequestParam(value = "reportType", defaultValue = "MEMBERS") String reportType,
                               @ModelAttribute("filter") ReportFilter filter,
                               Model model) {
        List<String> headers = reportService.getReportHeaders(reportType);
        List<List<String>> rows = reportService.getReportData(reportType, filter);

        model.addAttribute("reportType", reportType);
        model.addAttribute("headers", headers);
        model.addAttribute("rows", rows);
        model.addAttribute("filter", filter);
        return "reports/index";
    }

    @GetMapping("/reports/trial-balance")
    public String trialBalance(Model model) {
        Map<String, Object> trialBalance = reportService.generateTrialBalance();
        Map<String, Object> profitAndLoss = reportService.generateProfitAndLoss();

        model.addAttribute("trialBalance", trialBalance);
        model.addAttribute("profitAndLoss", profitAndLoss);
        return "reports/trial-balance";
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam("reportType") String reportType,
            @RequestParam("format") String format) throws Exception {

        List<String> headers = reportService.getReportHeaders(reportType);
        List<List<String>> rows = reportService.getReportData(reportType, new ReportFilter());

        if ("excel".equalsIgnoreCase(format)) {
            byte[] excelBytes = ReportExporter.exportToExcel(reportType, reportType + " Report", headers, rows);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportType.toLowerCase() + "_report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } else {
            byte[] pdfBytes = ReportExporter.exportToPdf(reportType + " REPORT", headers, rows);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportType.toLowerCase() + "_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }
    }

    // REST APIs
    @GetMapping("/api/reports/trial-balance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTrialBalanceApi() {
        return ResponseEntity.ok(reportService.generateTrialBalance());
    }

    @GetMapping("/api/reports/profit-loss")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProfitLossApi() {
        return ResponseEntity.ok(reportService.generateProfitAndLoss());
    }

    @GetMapping("/api/reports/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReportDataApi(@RequestParam String reportType) {
        List<String> headers = reportService.getReportHeaders(reportType);
        List<List<String>> rows = reportService.getReportData(reportType, new ReportFilter());
        return ResponseEntity.ok(Map.of("headers", headers, "rows", rows));
    }
}
