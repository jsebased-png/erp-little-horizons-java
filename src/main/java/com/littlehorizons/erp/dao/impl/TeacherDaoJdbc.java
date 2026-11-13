package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of TeacherDao (table profesores). */
public class TeacherDaoJdbc extends BaseJdbcDao implements TeacherDao {

    private static final String SELECT =
            "SELECT p.id_profesor, p.tipo_documento, p.numero_documento, p.primer_nombre, p.segundo_nombre, "
            + "p.primer_apellido, p.segundo_apellido, p.correo_electronico, p.telefono, p.especializacion, "
            + "p.fecha_contratacion, p.estado FROM profesores p ";

    public TeacherDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Teacher> findById(int id) {
        return queryOne(SELECT + "WHERE p.id_profesor = ?", TeacherDaoJdbc::map, id);
    }

    @Override
    public List<Teacher> findAll() {
        return queryList(SELECT + "ORDER BY p.primer_apellido, p.segundo_apellido, p.primer_nombre", TeacherDaoJdbc::map);
    }

    @Override
    public int insert(Teacher t) {
        int id = executeInsert("INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, "
                + "segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, "
                + "fecha_contratacion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", values(t));
        t.setId(id);
        return id;
    }

    @Override
    public boolean update(Teacher t) {
        return executeUpdate("UPDATE profesores SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, "
                + "segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, correo_electronico = ?, "
                + "telefono = ?, especializacion = ?, fecha_contratacion = ?, estado = ? WHERE id_profesor = ?",
                append(values(t), t.getId())) > 0;
    }

    @Override
    public boolean updateStatus(int teacherId, TeacherStatus status) {
        return executeUpdate("UPDATE profesores SET estado = ? WHERE id_profesor = ?", status, teacherId) > 0;
    }

    @Override
    public Optional<Teacher> findByDocument(String documentNumber) {
        return queryOne(SELECT + "WHERE p.numero_documento = ?", TeacherDaoJdbc::map, documentNumber);
    }

    private static Object[] values(Teacher t) {
        return new Object[] {
            t.getDocumentType(), t.getDocumentNumber(), t.getFirstName(), t.getMiddleName(), t.getFirstSurname(),
            t.getSecondSurname(), t.getEmail(), t.getPhone(), t.getSpecialization(), t.getHireDate(), t.getStatus()
        };
    }

    private static Teacher map(ResultSet rs) throws SQLException {
        Teacher t = new Teacher();
        t.setId(rs.getInt("id_profesor"));
        t.setDocumentType(EnumUtil.fromLabel(DocumentType.class, rs.getString("tipo_documento")));
        t.setDocumentNumber(rs.getString("numero_documento"));
        t.setFirstName(rs.getString("primer_nombre"));
        t.setMiddleName(rs.getString("segundo_nombre"));
        t.setFirstSurname(rs.getString("primer_apellido"));
        t.setSecondSurname(rs.getString("segundo_apellido"));
        t.setEmail(rs.getString("correo_electronico"));
        t.setPhone(rs.getString("telefono"));
        t.setSpecialization(rs.getString("especializacion"));
        t.setHireDate(rs.getObject("fecha_contratacion", LocalDate.class));
        t.setStatus(EnumUtil.fromLabel(TeacherStatus.class, rs.getString("estado")));
        return t;
    }
}
