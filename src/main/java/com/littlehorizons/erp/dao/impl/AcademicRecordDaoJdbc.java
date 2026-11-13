package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of AcademicRecordDao (table registros_academicos). */
public class AcademicRecordDaoJdbc extends BaseJdbcDao implements AcademicRecordDao {

    private static final String SELECT =
            "SELECT id_registro, id_estudiante, id_materia, id_profesor, `año_academico`, periodo_academico, "
            + "valor_nota, observaciones, fecha_registro FROM registros_academicos ";

    public AcademicRecordDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<AcademicRecord> findById(int id) {
        return queryOne(SELECT + "WHERE id_registro = ?", AcademicRecordDaoJdbc::map, id);
    }

    @Override
    public List<AcademicRecord> findAll() {
        return queryList(SELECT + "ORDER BY `año_academico`, id_estudiante, id_materia", AcademicRecordDaoJdbc::map);
    }

    @Override
    public int insert(AcademicRecord r) {
        int id = executeInsert("INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, "
                + "`año_academico`, periodo_academico, valor_nota, observaciones, fecha_registro) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                r.getStudentId(), r.getSubjectId(), r.getTeacherId(), r.getAcademicYear(), r.getPeriod(),
                r.getScore(), r.getObservations(), r.getRecordDate());
        r.setId(id);
        return id;
    }

    @Override
    public boolean update(AcademicRecord r) {
        return executeUpdate("UPDATE registros_academicos SET id_profesor = ?, valor_nota = ?, observaciones = ?, "
                + "fecha_registro = ? WHERE id_registro = ?",
                r.getTeacherId(), r.getScore(), r.getObservations(), r.getRecordDate(), r.getId()) > 0;
    }

    @Override
    public Optional<AcademicRecord> find(int studentId, int subjectId, int academicYear, AcademicPeriod period) {
        return queryOne(SELECT + "WHERE id_estudiante = ? AND id_materia = ? AND `año_academico` = ? "
                + "AND periodo_academico = ?", AcademicRecordDaoJdbc::map, studentId, subjectId, academicYear, period);
    }

    @Override
    public List<AcademicRecord> findByStudent(int studentId) {
        return queryList(SELECT + "WHERE id_estudiante = ? ORDER BY `año_academico`, id_materia, periodo_academico",
                AcademicRecordDaoJdbc::map, studentId);
    }

    @Override
    public List<AcademicRecord> findBySubjectAndYear(int subjectId, int academicYear) {
        return queryList(SELECT + "WHERE id_materia = ? AND `año_academico` = ? ORDER BY id_estudiante, periodo_academico",
                AcademicRecordDaoJdbc::map, subjectId, academicYear);
    }

    @Override
    public List<AcademicRecord> findByYear(int academicYear) {
        return queryList(SELECT + "WHERE `año_academico` = ? ORDER BY id_estudiante, id_materia",
                AcademicRecordDaoJdbc::map, academicYear);
    }

    private static AcademicRecord map(ResultSet rs) throws SQLException {
        AcademicRecord r = new AcademicRecord();
        r.setId(rs.getInt("id_registro"));
        r.setStudentId(rs.getInt("id_estudiante"));
        r.setSubjectId(rs.getInt("id_materia"));
        r.setTeacherId(rs.getInt("id_profesor"));
        r.setAcademicYear(rs.getInt("año_academico"));
        r.setPeriod(EnumUtil.fromLabel(AcademicPeriod.class, rs.getString("periodo_academico")));
        r.setScore(rs.getBigDecimal("valor_nota"));
        r.setObservations(rs.getString("observaciones"));
        r.setRecordDate(date(rs, "fecha_registro"));
        return r;
    }
}
