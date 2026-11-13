package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Gender;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of StudentDao (table estudiantes). */
public class StudentDaoJdbc extends BaseJdbcDao implements StudentDao {

    private static final String SELECT =
            "SELECT e.id_estudiante, e.tipo_documento, e.numero_documento, e.primer_nombre, e.segundo_nombre, "
            + "e.primer_apellido, e.segundo_apellido, e.fecha_nacimiento, e.genero, e.direccion, e.telefono, "
            + "e.correo_electronico, e.tipo_sangre, e.alergias, e.condiciones_medicas, e.fecha_ingreso, e.estado "
            + "FROM estudiantes e ";

    private static final String ORDER = "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";

    public StudentDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Student> findById(int id) {
        return queryOne(SELECT + "WHERE e.id_estudiante = ?", StudentDaoJdbc::map, id);
    }

    @Override
    public List<Student> findAll() {
        return queryList(SELECT + ORDER, StudentDaoJdbc::map);
    }

    @Override
    public int insert(Student s) {
        int id = executeInsert("INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, "
                + "segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, "
                + "correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", values(s));
        s.setId(id);
        return id;
    }

    @Override
    public boolean update(Student s) {
        return executeUpdate("UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, "
                + "segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, fecha_nacimiento = ?, genero = ?, "
                + "direccion = ?, telefono = ?, correo_electronico = ?, tipo_sangre = ?, alergias = ?, "
                + "condiciones_medicas = ?, fecha_ingreso = ?, estado = ? WHERE id_estudiante = ?",
                append(values(s), s.getId())) > 0;
    }

    @Override
    public boolean updateStatus(int studentId, StudentStatus status) {
        return executeUpdate("UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?", status, studentId) > 0;
    }

    @Override
    public Optional<Student> findByDocument(String documentNumber) {
        return queryOne(SELECT + "WHERE e.numero_documento = ?", StudentDaoJdbc::map, documentNumber);
    }

    @Override
    public List<Student> searchByName(String text) {
        return queryList(SELECT + "WHERE CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, "
                + "e.segundo_apellido) LIKE ? " + ORDER, StudentDaoJdbc::map, "%" + text + "%");
    }

    @Override
    public List<Student> findByGradeAndYear(int gradeId, int academicYear) {
        return queryList(SELECT + "JOIN matriculas m ON m.id_estudiante = e.id_estudiante "
                + "WHERE m.id_grado = ? AND m.`año_academico` = ? AND m.estado_matricula = ? " + ORDER,
                StudentDaoJdbc::map, gradeId, academicYear, EnrollmentStatus.ACTIVE);
    }

    /** Column values in the same order for INSERT and UPDATE (id excluded). */
    private static Object[] values(Student s) {
        return new Object[] {
            s.getDocumentType(), s.getDocumentNumber(), s.getFirstName(), s.getMiddleName(), s.getFirstSurname(),
            s.getSecondSurname(), s.getBirthDate(), s.getGender(), s.getAddress(), s.getPhone(), s.getEmail(),
            s.getBloodType(), s.getAllergies(), s.getMedicalConditions(), s.getAdmissionDate(), s.getStatus()
        };
    }

    private static Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id_estudiante"));
        s.setDocumentType(EnumUtil.fromLabel(DocumentType.class, rs.getString("tipo_documento")));
        s.setDocumentNumber(rs.getString("numero_documento"));
        s.setFirstName(rs.getString("primer_nombre"));
        s.setMiddleName(rs.getString("segundo_nombre"));
        s.setFirstSurname(rs.getString("primer_apellido"));
        s.setSecondSurname(rs.getString("segundo_apellido"));
        s.setBirthDate(rs.getObject("fecha_nacimiento", LocalDate.class));
        s.setGender(EnumUtil.fromLabel(Gender.class, rs.getString("genero")));
        s.setAddress(rs.getString("direccion"));
        s.setPhone(rs.getString("telefono"));
        s.setEmail(rs.getString("correo_electronico"));
        s.setBloodType(rs.getString("tipo_sangre"));
        s.setAllergies(rs.getString("alergias"));
        s.setMedicalConditions(rs.getString("condiciones_medicas"));
        s.setAdmissionDate(rs.getObject("fecha_ingreso", LocalDate.class));
        s.setStatus(EnumUtil.fromLabel(StudentStatus.class, rs.getString("estado")));
        return s;
    }
}
