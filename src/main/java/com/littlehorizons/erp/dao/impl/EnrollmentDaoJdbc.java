package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of EnrollmentDao (table matriculas). */
public class EnrollmentDaoJdbc extends BaseJdbcDao implements EnrollmentDao {

    private static final String SELECT =
            "SELECT id_matricula, id_estudiante, id_grado, `año_academico`, estado_matricula, fecha_matricula, "
            + "fecha_retiro, observaciones FROM matriculas ";

    public EnrollmentDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Enrollment> findById(int id) {
        return queryOne(SELECT + "WHERE id_matricula = ?", EnrollmentDaoJdbc::map, id);
    }

    @Override
    public List<Enrollment> findAll() {
        return queryList(SELECT + "ORDER BY `año_academico` DESC, id_matricula", EnrollmentDaoJdbc::map);
    }

    @Override
    public int insert(Enrollment e) {
        int id = executeInsert("INSERT INTO matriculas (id_estudiante, id_grado, `año_academico`, estado_matricula, "
                + "fecha_matricula, fecha_retiro, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?)",
                e.getStudentId(), e.getGradeId(), e.getAcademicYear(), e.getStatus(), e.getEnrollmentDate(),
                e.getWithdrawalDate(), e.getNotes());
        e.setId(id);
        return id;
    }

    @Override
    public boolean updateStatus(int enrollmentId, EnrollmentStatus status, LocalDate withdrawalDate) {
        return executeUpdate("UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ? WHERE id_matricula = ?",
                status, withdrawalDate, enrollmentId) > 0;
    }

    @Override
    public List<Enrollment> findByStudent(int studentId) {
        return queryList(SELECT + "WHERE id_estudiante = ? ORDER BY `año_academico` DESC",
                EnrollmentDaoJdbc::map, studentId);
    }

    @Override
    public List<Enrollment> findByYear(int academicYear) {
        return queryList(SELECT + "WHERE `año_academico` = ? ORDER BY id_grado, id_estudiante",
                EnrollmentDaoJdbc::map, academicYear);
    }

    @Override
    public Optional<Enrollment> findByStudentAndYear(int studentId, int academicYear) {
        return queryOne(SELECT + "WHERE id_estudiante = ? AND `año_academico` = ?",
                EnrollmentDaoJdbc::map, studentId, academicYear);
    }

    private static Enrollment map(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setId(rs.getInt("id_matricula"));
        e.setStudentId(rs.getInt("id_estudiante"));
        e.setGradeId(rs.getInt("id_grado"));
        e.setAcademicYear(rs.getInt("año_academico"));
        e.setStatus(EnumUtil.fromLabel(EnrollmentStatus.class, rs.getString("estado_matricula")));
        e.setEnrollmentDate(date(rs, "fecha_matricula"));
        e.setWithdrawalDate(date(rs, "fecha_retiro"));
        e.setNotes(rs.getString("observaciones"));
        return e;
    }
}
