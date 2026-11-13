package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of GuardianDao (tables acudientes and estudiante_acudiente). */
public class GuardianDaoJdbc extends BaseJdbcDao implements GuardianDao {

    private static final String COLUMNS =
            "SELECT a.id_acudiente, a.tipo_documento, a.numero_documento, a.primer_nombre, a.segundo_nombre, "
            + "a.primer_apellido, a.segundo_apellido, a.parentesco, a.telefono_principal, a.telefono_secundario, "
            + "a.correo_electronico, a.direccion, a.ocupacion, a.es_responsable_financiero";

    private static final String SELECT = COLUMNS + " FROM acudientes a ";

    private static final String SELECT_LINKED = COLUMNS + ", ea.es_principal, ea.es_contacto_emergencia "
            + "FROM acudientes a JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente ";

    public GuardianDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Guardian> findById(int id) {
        return queryOne(SELECT + "WHERE a.id_acudiente = ?", GuardianDaoJdbc::map, id);
    }

    @Override
    public List<Guardian> findAll() {
        return queryList(SELECT + "ORDER BY a.primer_apellido, a.segundo_apellido, a.primer_nombre", GuardianDaoJdbc::map);
    }

    @Override
    public int insert(Guardian g) {
        int id = executeInsert("INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, "
                + "segundo_nombre, primer_apellido, segundo_apellido, parentesco, telefono_principal, "
                + "telefono_secundario, correo_electronico, direccion, ocupacion, es_responsable_financiero) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", values(g));
        g.setId(id);
        return id;
    }

    @Override
    public boolean update(Guardian g) {
        return executeUpdate("UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, "
                + "segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, parentesco = ?, "
                + "telefono_principal = ?, telefono_secundario = ?, correo_electronico = ?, direccion = ?, "
                + "ocupacion = ?, es_responsable_financiero = ? WHERE id_acudiente = ?",
                append(values(g), g.getId())) > 0;
    }

    @Override
    public Optional<Guardian> findByDocument(String documentNumber) {
        return queryOne(SELECT + "WHERE a.numero_documento = ?", GuardianDaoJdbc::map, documentNumber);
    }

    @Override
    public boolean link(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        return executeUpdate("INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, "
                + "es_contacto_emergencia) VALUES (?, ?, ?, ?)", studentId, guardianId, primary, emergencyContact) > 0;
    }

    @Override
    public boolean unlink(int studentId, int guardianId) {
        return executeUpdate("DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?",
                studentId, guardianId) > 0;
    }

    @Override
    public List<Guardian> findByStudentId(int studentId) {
        return queryList(SELECT_LINKED + "WHERE ea.id_estudiante = ? ORDER BY ea.es_principal DESC, a.primer_apellido",
                GuardianDaoJdbc::mapLinked, studentId);
    }

    @Override
    public Optional<Guardian> findFinancialGuardian(int studentId) {
        return queryOne(SELECT_LINKED + "WHERE ea.id_estudiante = ? AND a.es_responsable_financiero = TRUE "
                + "ORDER BY ea.es_principal DESC LIMIT 1", GuardianDaoJdbc::mapLinked, studentId);
    }

    @Override
    public int countByStudent(int studentId) {
        return scalarInt("SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ?", studentId);
    }

    @Override
    public int countActiveStudents(int guardianId) {
        return scalarInt("SELECT COUNT(*) FROM estudiante_acudiente ea "
                + "JOIN estudiantes e ON e.id_estudiante = ea.id_estudiante "
                + "WHERE ea.id_acudiente = ? AND e.estado = ?", guardianId, StudentStatus.ACTIVE);
    }

    private static Object[] values(Guardian g) {
        return new Object[] {
            g.getDocumentType(), g.getDocumentNumber(), g.getFirstName(), g.getMiddleName(), g.getFirstSurname(),
            g.getSecondSurname(), g.getRelationship(), g.getPrimaryPhone(), g.getSecondaryPhone(), g.getEmail(),
            g.getAddress(), g.getOccupation(), g.isFinanciallyResponsible()
        };
    }

    private static Guardian map(ResultSet rs) throws SQLException {
        Guardian g = new Guardian();
        g.setId(rs.getInt("id_acudiente"));
        g.setDocumentType(EnumUtil.fromLabel(DocumentType.class, rs.getString("tipo_documento")));
        g.setDocumentNumber(rs.getString("numero_documento"));
        g.setFirstName(rs.getString("primer_nombre"));
        g.setMiddleName(rs.getString("segundo_nombre"));
        g.setFirstSurname(rs.getString("primer_apellido"));
        g.setSecondSurname(rs.getString("segundo_apellido"));
        g.setRelationship(rs.getString("parentesco"));
        g.setPrimaryPhone(rs.getString("telefono_principal"));
        g.setSecondaryPhone(rs.getString("telefono_secundario"));
        g.setEmail(rs.getString("correo_electronico"));
        g.setAddress(rs.getString("direccion"));
        g.setOccupation(rs.getString("ocupacion"));
        g.setFinanciallyResponsible(rs.getBoolean("es_responsable_financiero"));
        return g;
    }

    /** Same as map, plus the flags that belong to the student-guardian link. */
    private static Guardian mapLinked(ResultSet rs) throws SQLException {
        Guardian g = map(rs);
        g.setPrimaryContact(rs.getBoolean("es_principal"));
        g.setEmergencyContact(rs.getBoolean("es_contacto_emergencia"));
        return g;
    }
}
