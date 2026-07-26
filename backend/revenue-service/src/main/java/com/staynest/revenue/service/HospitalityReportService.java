package com.staynest.revenue.service;

import com.staynest.revenue.dto.HospitalityReportRequest;
import com.staynest.revenue.dto.HospitalityReportResponse;
import com.staynest.revenue.dto.KpiSummaryResponse;

import java.util.List;

public interface HospitalityReportService {
    HospitalityReportResponse generateReport(HospitalityReportRequest request);
    HospitalityReportResponse getReportById(Integer id);
    List<HospitalityReportResponse> getAllReports();
    List<HospitalityReportResponse> getReportsByScope(String scope);
    KpiSummaryResponse getSummary();
}