package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.LoanStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Contract for resource loans. There is one implementation per table
 * (students and teachers), both behave the same way from the caller's point of view.
 */
public interface LoanDao extends ReadableDao<Loan> {

    BorrowerType getBorrowerType();

    /**
     * Saves the loan and marks the resource as LOANED in one transaction.
     * Returns false (and saves nothing) if the resource was not available.
     */
    boolean registerLoan(Loan loan);

    /**
     * Marks the loan as returned and the resource as AVAILABLE in one transaction.
     * Returns false if the loan does not exist or was already returned.
     */
    boolean returnLoan(int loanId, LocalDate returnDate);

    List<Loan> findByBorrower(int borrowerId);

    List<Loan> findByStatus(LoanStatus status);

    /** Loans not returned yet (ACTIVE or OVERDUE). */
    List<Loan> findOutstanding();

    /** Changes ACTIVE loans whose expected date is before today to OVERDUE. Returns how many changed. */
    int markOverdue(LocalDate today);
}
