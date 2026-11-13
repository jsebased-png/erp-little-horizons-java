package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;

import java.util.List;

/** Data access contract for payments. */
public interface PaymentDao extends ReadableDao<Payment> {

    /**
     * Saves the payment AND its invoice in one transaction.
     * If either insert fails, nothing is saved. Generated ids are set on both objects.
     */
    void registerWithInvoice(Payment payment, Invoice invoice);

    List<Payment> findByStudent(int studentId);

    List<Payment> findByGuardian(int guardianId);

    /** Payments of one concept code (for example PENSION), used by the overdue report. */
    List<Payment> findByConceptCode(String conceptCode);

    /** Next payment number (last id + 1), used to build receipt numbers. */
    int nextSequence();
}
