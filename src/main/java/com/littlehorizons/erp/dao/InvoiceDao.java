package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;

import java.util.Optional;

/** Data access contract for invoices. Creation goes through PaymentDao (same transaction as the payment). */
public interface InvoiceDao extends ReadableDao<Invoice> {

    Optional<Invoice> findByPayment(int paymentId);

    Optional<Invoice> findByNumber(String invoiceNumber);

    /** Stores the PDF name and path once the file exists on disk. */
    boolean updateFileInfo(int invoiceId, String fileName, String path);

    /** Next invoice consecutive (last id + 1). */
    int nextSequence();
}
