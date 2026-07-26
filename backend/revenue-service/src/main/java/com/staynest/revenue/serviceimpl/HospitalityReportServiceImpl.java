package com.staynest.revenue.serviceimpl;

import com.staynest.revenue.client.ReservationServiceClient;
import com.staynest.revenue.client.RoomServiceClient;
import com.staynest.revenue.dto.HospitalityReportRequest;
import com.staynest.revenue.dto.HospitalityReportResponse;
import com.staynest.revenue.dto.KpiSummaryResponse;
import com.staynest.revenue.entity.HospitalityReport;
import com.staynest.revenue.exception.ResourceNotFoundException;
import com.staynest.revenue.repository.HospitalityReportRepository;
import com.staynest.revenue.service.HospitalityReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalityReportServiceImpl implements HospitalityReportService {

    private final HospitalityReportRepository reportRepository;
    private final RoomServiceClient roomServiceClient;
    private final ReservationServiceClient reservationServiceClient;

    @Override
    public HospitalityReportResponse generateReport(HospitalityReportRequest request) {
        HospitalityReport report = HospitalityReport.builder()
                .scope(request.getScope())
                .metrics(request.getMetrics())
                .build();

        HospitalityReport saved = reportRepository.save(report);
        log.info("Hospitality report generated: {}", saved.getReportId());
        return mapToResponse(saved);
    }

    @Override
    public HospitalityReportResponse getReportById(Integer id) {
        HospitalityReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        return mapToResponse(report);
    }

    @Override
    public List<HospitalityReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<HospitalityReportResponse> getReportsByScope(String scope) {
        return reportRepository.findByScope(scope).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public KpiSummaryResponse getSummary() {
        // Phase 1: Hardcoded KPIs (can be enhanced with real aggregation from Feign clients)
        // In production, fetch real data from Room and Reservation services
        
        KpiSummaryResponse summary = KpiSummaryResponse.builder()
                .occupancyRate(78.0)
                .adr(new BigDecimal("4500.00"))
                .revPAR(new BigDecimal("3510.00"))
                .avgLengthOfStay(2.5)
                .fbRevenue(new BigDecimal("125000.00"))
                .guestSatisfactionScore(4.2)
                .build();

        log.info("KPI summary generated");
        return summary;
    }

    private HospitalityReportResponse mapToResponse(HospitalityReport report) {
        return HospitalityReportResponse.builder()
                .reportId(report.getReportId())
                .scope(report.getScope())
                .metrics(report.getMetrics())
                .generatedDate(report.getGeneratedDate())
                .build();
    }
}