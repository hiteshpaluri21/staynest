package com.staynest.room.service;

import com.staynest.room.entity.HospitalityReport;

import java.util.List;

public interface HospitalityReportService {

	HospitalityReport generateReport(String scope, String metrics);
	List<HospitalityReport> getAllReports();
	List<HospitalityReport> getReportsByScope(String scope);
	HospitalityReport getReportById(Integer id);
}