package com.staynest.room.controller;

import com.staynest.room.dto.ApiResponse;
import com.staynest.room.entity.HospitalityReport;
import com.staynest.room.service.HospitalityReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitality-reports")
@RequiredArgsConstructor
public class HospitalityReportController {

    private final HospitalityReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<HospitalityReport>> generate(
            @RequestParam String scope,
            @RequestParam String metrics) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated", reportService.generateReport(scope, metrics)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HospitalityReport>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getAllReports()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HospitalityReport>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReportById(id)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<String>> getSummary() {
        // Hardcoded summary for Phase 1
        String summary = "{\"occupancyRate\":78,\"adr\":4500,\"revPAR\":3510,\"guestScore\":4.2}";
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}