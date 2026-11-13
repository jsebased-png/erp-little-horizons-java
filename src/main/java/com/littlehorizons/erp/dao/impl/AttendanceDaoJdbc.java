package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of AttendanceDao (table asistencias). */
public class AttendanceDaoJdbc extends BaseJdbcDao implements AttendanceDao {

    private static final String SELECT =
            "SELECT id_asistencia, id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, "
            + "registrado_por FROM asistencias ";

    public AttendanceDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Attendance> findById(int id) {
        return queryOne(SELECT + "WHERE id_asistencia = ?", AttendanceDaoJdbc::map, id);
    }

    @Override
    public List<Attendance> findAll() {
        return queryList(SELECT + "ORDER BY fecha_asistencia DESC, id_estudiante", AttendanceDaoJdbc::map);
    }

    @Override
    public int insert(Attendance a) {
        int id = executeInsert("INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, "
                + "estado_asistencia, observaciones, registrado_por) VALUES (?, ?, ?, ?, ?, ?)",
                a.getStudentId(), a.getSubjectId(), a.getDate(), a.getStatus(), a.getObservations(),
                a.getRegisteredBy());
        a.setId(id);
        return id;
    }

    @Override
    public boolean update(Attendance a) {
        return executeUpdate("UPDATE asistencias SET estado_asistencia = ?, observaciones = ?, registrado_por = ? "
                + "WHERE id_asistencia = ?", a.getStatus(), a.getObservations(), a.getRegisteredBy(), a.getId()) > 0;
    }

    @Override
    public Optional<Attendance> find(int studentId, int subjectId, LocalDate date) {
        return queryOne(SELECT + "WHERE id_estudiante = ? AND id_materia = ? AND fecha_asistencia = ?",
                AttendanceDaoJdbc::map, studentId, subjectId, date);
    }

    @Override
    public List<Attendance> findByStudent(int studentId) {
        return queryList(SELECT + "WHERE id_estudiante = ? ORDER BY fecha_asistencia DESC",
                AttendanceDaoJdbc::map, studentId);
    }

    @Override
    public List<Attendance> findBetween(LocalDate from, LocalDate to) {
        return queryList(SELECT + "WHERE fecha_asistencia BETWEEN ? AND ? ORDER BY fecha_asistencia",
                AttendanceDaoJdbc::map, from, to);
    }

    private static Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id_asistencia"));
        a.setStudentId(rs.getInt("id_estudiante"));
        a.setSubjectId(rs.getInt("id_materia"));
        a.setDate(date(rs, "fecha_asistencia"));
        a.setStatus(EnumUtil.fromLabel(AttendanceStatus.class, rs.getString("estado_asistencia")));
        a.setObservations(rs.getString("observaciones"));
        a.setRegisteredBy(nullableInt(rs, "registrado_por"));
        return a;
    }
}
