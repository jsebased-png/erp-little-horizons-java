package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of SubjectEnrollmentDao (table inscripciones_materia). */
public class SubjectEnrollmentDaoJdbc extends BaseJdbcDao implements SubjectEnrollmentDao {

    private static final String SELECT =
            "SELECT id_inscripcion, id_estudiante, id_materia, `año_academico`, periodo_academico, seccion, "
            + "fecha_inscripcion, estado FROM inscripciones_materia ";

    public SubjectEnrollmentDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<SubjectEnrollment> findById(int id) {
        return queryOne(SELECT + "WHERE id_inscripcion = ?", SubjectEnrollmentDaoJdbc::map, id);
    }

    @Override
    public List<SubjectEnrollment> findAll() {
        return queryList(SELECT + "ORDER BY `año_academico` DESC, id_estudiante", SubjectEnrollmentDaoJdbc::map);
    }

    @Override
    public int insert(SubjectEnrollment e) {
        int id = executeInsert("INSERT INTO inscripciones_materia (id_estudiante, id_materia, `año_academico`, "
                + "periodo_academico, seccion, fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, ?, ?)",
                e.getStudentId(), e.getSubjectId(), e.getAcademicYear(), e.getPeriod(), e.getSection(),
                e.getEnrollmentDate(), e.getStatus());
        e.setId(id);
        return id;
    }

    @Override
    public boolean updateStatus(int enrollmentId, EnrollmentStatus status) {
        return executeUpdate("UPDATE inscripciones_materia SET estado = ? WHERE id_inscripcion = ?",
                status, enrollmentId) > 0;
    }

    @Override
    public boolean exists(int studentId, int subjectId, int academicYear, AcademicPeriod period) {
        return scalarInt("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? "
                + "AND `año_academico` = ? AND periodo_academico = ?", studentId, subjectId, academicYear, period) > 0;
    }

    @Override
    public List<SubjectEnrollment> findByStudentAndYear(int studentId, int academicYear) {
        return queryList(SELECT + "WHERE id_estudiante = ? AND `año_academico` = ? ORDER BY periodo_academico, id_materia",
                SubjectEnrollmentDaoJdbc::map, studentId, academicYear);
    }

    @Override
    public List<SubjectEnrollment> findBySubjectAndPeriod(int subjectId, int academicYear, AcademicPeriod period) {
        return queryList(SELECT + "WHERE id_materia = ? AND `año_academico` = ? AND periodo_academico = ?",
                SubjectEnrollmentDaoJdbc::map, subjectId, academicYear, period);
    }

    private static SubjectEnrollment map(ResultSet rs) throws SQLException {
        SubjectEnrollment e = new SubjectEnrollment();
        e.setId(rs.getInt("id_inscripcion"));
        e.setStudentId(rs.getInt("id_estudiante"));
        e.setSubjectId(rs.getInt("id_materia"));
        e.setAcademicYear(rs.getInt("año_academico"));
        e.setPeriod(EnumUtil.fromLabel(AcademicPeriod.class, rs.getString("periodo_academico")));
        e.setSection(rs.getString("seccion"));
        e.setEnrollmentDate(date(rs, "fecha_inscripcion"));
        e.setStatus(EnumUtil.fromLabel(EnrollmentStatus.class, rs.getString("estado")));
        return e;
    }
}
