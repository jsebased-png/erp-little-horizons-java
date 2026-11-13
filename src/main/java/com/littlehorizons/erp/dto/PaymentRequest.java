package com.littlehorizons.erp.dto;

import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.PaymentMethod;

import java.math.BigDecimal;

/**
 * Data the view collects to register a payment.
 * It exists so registerPayment receives one object instead of eight parameters.
 *
 * @param guardianId         guardian who pays; null means "the financial guardian of the student"
 * @param scholarshipPercent scholarship percentage (0-100); null or zero means no scholarship
 */
public record PaymentRequest(int studentId, int conceptId, Integer guardianId, PaymentMethod method,
                             AcademicPeriod period, Integer academicYear, BigDecimal scholarshipPercent) {
}
