package com.staynest.revenue.controller;

import com.staynest.revenue.dto.ApiResponse;
import com.staynest.revenue.dto.HospitalityReportRequest;
import com.staynest.revenue.dto.HospitalityReportResponse;
import com.staynest.revenue.dto.KpiSummaryResponse;
import com.staynest.revenue.service.HospitalityReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class HospitalityReportController {

    private final HospitalityReportService reportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVENUEMANAGER')")
    public ResponseEntity<ApiResponse<HospitalityReportResponse>> generate(
            @Valid @RequestBody HospitalityReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated", reportService.generateReport(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVENUEMANAGER')")
    public ResponseEntity<ApiResponse<List<HospitalityReportResponse>>> getAll(
            @RequestParam(required = false) String scope) {
        if (scope != null) {
            return ResponseEntity.ok(ApiResponse.success(reportService.getReportsByScope(scope)));
        }
        return ResponseEntity.ok(ApiResponse.success(reportService.getAllReports()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVENUEMANAGER')")
    public ResponseEntity<ApiResponse<KpiSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSummary()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVENUEMANAGER')")
    public ResponseEntity<ApiResponse<HospitalityReportResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReportById(id)));
    }
}