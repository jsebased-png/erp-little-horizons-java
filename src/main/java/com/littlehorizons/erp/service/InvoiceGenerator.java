package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dto.PaymentResult;

import java.nio.file.Path;

/**
 * Writes the electronic invoice of a payment to a file.
 * It exists so controllers ask for "an invoice file" without knowing the concrete format (TXT by default).
 */
// SOLID: DIP – InvoiceController depends on this interface, not on a concrete writer
// SOLID: OCP – another format (PDF, HTML) is a new implementation; the controller does not change
public interface InvoiceGenerator {

    /**
     * Creates (or overwrites) the file at target. Parent folders are created when missing.
     *
     * @throws com.littlehorizons.erp.exception.FileOperationException if the file cannot be written
     */
    void generate(PaymentResult result, Path target);
}
