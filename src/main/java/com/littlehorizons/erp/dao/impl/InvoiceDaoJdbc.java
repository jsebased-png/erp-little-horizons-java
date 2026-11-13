package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.model.Invoice;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of InvoiceDao (table facturas). */
public class InvoiceDaoJdbc extends BaseJdbcDao implements InvoiceDao {

    private static final String SELECT =
            "SELECT id_factura, numero_factura, id_pago, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo "
            + "FROM facturas ";

    public InvoiceDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Invoice> findById(int id) {
        return queryOne(SELECT + "WHERE id_factura = ?", InvoiceDaoJdbc::map, id);
    }

    @Override
    public List<Invoice> findAll() {
        return queryList(SELECT + "ORDER BY id_factura DESC", InvoiceDaoJdbc::map);
    }

    @Override
    public Optional<Invoice> findByPayment(int paymentId) {
        return queryOne(SELECT + "WHERE id_pago = ?", InvoiceDaoJdbc::map, paymentId);
    }

    @Override
    public Optional<Invoice> findByNumber(String invoiceNumber) {
        return queryOne(SELECT + "WHERE numero_factura = ?", InvoiceDaoJdbc::map, invoiceNumber);
    }

    @Override
    public boolean updateFileInfo(int invoiceId, String fileName, String path) {
        return executeUpdate("UPDATE facturas SET nombre_archivo_pdf = ?, ruta_archivo = ? WHERE id_factura = ?",
                fileName, path, invoiceId) > 0;
    }

    @Override
    public int nextSequence() {
        return scalarInt("SELECT COALESCE(MAX(id_factura), 0) + 1 FROM facturas");
    }

    private static Invoice map(ResultSet rs) throws SQLException {
        Invoice i = new Invoice();
        i.setId(rs.getInt("id_factura"));
        i.setInvoiceNumber(rs.getString("numero_factura"));
        i.setPaymentId(rs.getInt("id_pago"));
        i.setIssueDate(date(rs, "fecha_emision"));
        i.setCufe(rs.getString("cufe"));
        i.setPdfFileName(rs.getString("nombre_archivo_pdf"));
        i.setPdfPath(rs.getString("ruta_archivo"));
        return i;
    }
}
