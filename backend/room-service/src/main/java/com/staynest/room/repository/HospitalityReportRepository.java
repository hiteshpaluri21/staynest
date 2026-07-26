package com.staynest.room.repository;

import com.staynest.room.entity.HospitalityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalityReportRepository extends JpaRepository<HospitalityReport, Integer> {
    List<HospitalityReport> findByScope(String scope);
}