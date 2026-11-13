package com.littlehorizons.erp.dto;

import com.littlehorizons.erp.model.BorrowerType;

import java.time.LocalDate;

/** A loan not returned after its expected date, with readable names for the report. */
public record OverdueLoan(BorrowerType borrowerType, int loanId, String resourceCode, String resourceName,
                          String borrowerName, LocalDate expectedReturnDate, long daysLate) {
}
