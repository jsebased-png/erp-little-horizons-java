package com.littlehorizons.erp.dto;

import java.time.LocalDate;

/**
 * A student with too many absent days in one school week (Monday to Sunday).
 *
 * @param weekStart  Monday of the week
 * @param absentDays number of different days with at least one absence
 */
public record AbsenceAlert(int studentId, String studentName, LocalDate weekStart, int absentDays) {
}
