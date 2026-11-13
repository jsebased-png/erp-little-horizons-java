package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of PaymentDao (tables pagos and facturas). */
public class PaymentDaoJdbc extends BaseJdbcDao implements PaymentDao {

    private static final String SELECT =
            "SELECT p.id_pago, p.id_estudiante, p.id_acudiente, p.id_concepto, p.monto_pagado, p.fecha_pago, "
            + "p.metodo_pago, p.numero_recibo, p.`año_academico`, p.periodo_academico, p.notas FROM pagos p ";

    private static final String INSERT_PAYMENT =
            "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, metodo_pago, "
            + "numero_recibo, `año_academico`, periodo_academico, notas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_INVOICE =
            "INSERT INTO facturas (numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    public PaymentDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Payment> findById(int id) {
        return queryOne(SELECT + "WHERE p.id_pago = ?", PaymentDaoJdbc::map, id);
    }

    @Override
    public List<Payment> findAll() {
        return queryList(SELECT + "ORDER BY p.fecha_pago DESC, p.id_pago DESC", PaymentDaoJdbc::map);
    }

    @Override
    public void registerWithInvoice(Payment payment, Invoice invoice) {
        // Both inserts share ONE connection: if the invoice fails, the payment is rolled back.
        inTransaction(connection -> {
            int paymentId = executeInsert(connection, INSERT_PAYMENT,
                    payment.getStudentId(), payment.getGuardianId(), payment.getConceptId(), payment.getAmountPaid(),
                    payment.getPaymentDate(), payment.getMethod(), payment.getReceiptNumber(),
                    payment.getAcademicYear(), payment.getPeriod(), payment.getNotes());
            payment.setId(paymentId);

            invoice.setPaymentId(paymentId);
            int invoiceId = executeInsert(connection, INSERT_INVOICE,
                    invoice.getInvoiceNumber(), paymentId, invoice.getIssueDate(), invoice.getCufe(),
                    invoice.getPdfFileName(), invoice.getPdfPath());
            invoice.setId(invoiceId);
            return null;
        });
    }

    @Override
    public List<Payment> findByStudent(int studentId) {
        return queryList(SELECT + "WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC", PaymentDaoJdbc::map, studentId);
    }

    @Override
    public List<Payment> findByGuardian(int guardianId) {
        return queryList(SELECT + "WHERE p.id_acudiente = ? ORDER BY p.fecha_pago DESC", PaymentDaoJdbc::map, guardianId);
    }

    @Override
    public List<Payment> findByConceptCode(String conceptCode) {
        return queryList(SELECT + "JOIN conceptos_pago c ON c.id_concepto = p.id_concepto "
                + "WHERE c.codigo_concepto = ? ORDER BY p.fecha_pago DESC", PaymentDaoJdbc::map, conceptCode);
    }

    @Override
    public int nextSequence() {
        return scalarInt("SELECT COALESCE(MAX(id_pago), 0) + 1 FROM pagos");
    }

    private static Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id_pago"));
        p.setStudentId(rs.getInt("id_estudiante"));
        p.setGuardianId(nullableInt(rs, "id_acudiente"));
        p.setConceptId(rs.getInt("id_concepto"));
        p.setAmountPaid(rs.getBigDecimal("monto_pagado"));
        p.setPaymentDate(date(rs, "fecha_pago"));
        p.setMethod(EnumUtil.fromLabel(PaymentMethod.class, rs.getString("metodo_pago")));
        p.setReceiptNumber(rs.getString("numero_recibo"));
        p.setAcademicYear(nullableInt(rs, "año_academico"));
        p.setPeriod(EnumUtil.fromLabel(AcademicPeriod.class, rs.getString("periodo_academico")));
        p.setNotes(rs.getString("notas"));
        return p;
    }
}
