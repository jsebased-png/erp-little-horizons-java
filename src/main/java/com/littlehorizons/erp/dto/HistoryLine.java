package com.littlehorizons.erp.dto;

import com.littlehorizons.erp.model.AcademicPeriod;

import java.math.BigDecimal;

/** One line of a student's academic history. */
public record HistoryLine(int academicYear, String subjectName, AcademicPeriod period, BigDecimal score) {
}
