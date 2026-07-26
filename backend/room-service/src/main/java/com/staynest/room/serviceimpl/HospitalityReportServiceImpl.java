package com.staynest.room.serviceimpl;

import com.staynest.room.entity.HospitalityReport;
import com.staynest.room.exception.ResourceNotFoundException;
import com.staynest.room.repository.HospitalityReportRepository;
import com.staynest.room.service.HospitalityReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HospitalityReportServiceImpl implements HospitalityReportService {

    private static final Logger log = LoggerFactory.getLogger(HospitalityReportServiceImpl.class);

    @Autowired
    private HospitalityReportRepository reportRepository;

    @Override
    public HospitalityReport generateReport(String scope, String metrics) {
        HospitalityReport report = new HospitalityReport();
        report.setScope(scope);
        report.setMetrics(metrics);

        HospitalityReport saved = reportRepository.save(report);
        log.info("Report generated: {}", saved.getReportId());
        return saved;
    }

    @Override
    public List<HospitalityReport> getAllReports() {
        return reportRepository.findAll();
    }

    @Override
    public List<HospitalityReport> getReportsByScope(String scope) {
        return reportRepository.findByScope(scope);
    }

    @Override
    public HospitalityReport getReportById(Integer id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }
}