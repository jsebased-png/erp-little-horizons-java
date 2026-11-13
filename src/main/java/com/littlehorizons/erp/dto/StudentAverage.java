package com.littlehorizons.erp.dto;

import java.math.BigDecimal;

/** Average of one student, used by the performance ranking. */
public record StudentAverage(int studentId, String studentName, BigDecimal average) {
}
