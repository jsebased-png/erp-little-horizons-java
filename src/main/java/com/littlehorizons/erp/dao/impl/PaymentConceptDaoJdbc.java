package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.model.PaymentConcept;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of PaymentConceptDao (table conceptos_pago). */
public class PaymentConceptDaoJdbc extends BaseJdbcDao implements PaymentConceptDao {

    private static final String SELECT =
            "SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, esta_activo "
            + "FROM conceptos_pago ";

    public PaymentConceptDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<PaymentConcept> findById(int id) {
        return queryOne(SELECT + "WHERE id_concepto = ?", PaymentConceptDaoJdbc::map, id);
    }

    @Override
    public List<PaymentConcept> findAll() {
        return queryList(SELECT + "ORDER BY nombre_concepto", PaymentConceptDaoJdbc::map);
    }

    @Override
    public int insert(PaymentConcept c) {
        int id = executeInsert("INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, "
                + "monto_base, es_recurrente, esta_activo) VALUES (?, ?, ?, ?, ?, ?)", values(c));
        c.setId(id);
        return id;
    }

    @Override
    public boolean update(PaymentConcept c) {
        return executeUpdate("UPDATE conceptos_pago SET codigo_concepto = ?, nombre_concepto = ?, descripcion = ?, "
                + "monto_base = ?, es_recurrente = ?, esta_activo = ? WHERE id_concepto = ?",
                append(values(c), c.getId())) > 0;
    }

    @Override
    public boolean setActive(int conceptId, boolean active) {
        return executeUpdate("UPDATE conceptos_pago SET esta_activo = ? WHERE id_concepto = ?", active, conceptId) > 0;
    }

    @Override
    public Optional<PaymentConcept> findByCode(String code) {
        return queryOne(SELECT + "WHERE codigo_concepto = ?", PaymentConceptDaoJdbc::map, code);
    }

    @Override
    public List<PaymentConcept> findActive() {
        return queryList(SELECT + "WHERE esta_activo = TRUE ORDER BY nombre_concepto", PaymentConceptDaoJdbc::map);
    }

    private static Object[] values(PaymentConcept c) {
        return new Object[] {
            c.getCode(), c.getName(), c.getDescription(), c.getBaseAmount(), c.isRecurring(), c.isActive()
        };
    }

    private static PaymentConcept map(ResultSet rs) throws SQLException {
        PaymentConcept c = new PaymentConcept();
        c.setId(rs.getInt("id_concepto"));
        c.setCode(rs.getString("codigo_concepto"));
        c.setName(rs.getString("nombre_concepto"));
        c.setDescription(rs.getString("descripcion"));
        c.setBaseAmount(rs.getBigDecimal("monto_base"));
        c.setRecurring(rs.getBoolean("es_recurrente"));
        c.setActive(rs.getBoolean("esta_activo"));
        return c;
    }
}
