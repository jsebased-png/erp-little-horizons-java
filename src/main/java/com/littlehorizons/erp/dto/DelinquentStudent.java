package com.littlehorizons.erp.dto;

import java.time.LocalDate;

/**
 * A student with an active enrollment whose monthly tuition is overdue.
 *
 * @param referenceDate last tuition payment, or the enrollment date if the student never paid
 * @param daysOverdue   days elapsed since the reference date
 */
public record DelinquentStudent(int studentId, String studentName, LocalDate referenceDate,
                                long daysOverdue, boolean neverPaid) {
}
