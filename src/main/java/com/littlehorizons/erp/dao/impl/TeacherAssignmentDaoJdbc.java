package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.model.WeekDay;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of TeacherAssignmentDao (tables asignaciones_profesor and horarios_asignacion). */
public class TeacherAssignmentDaoJdbc extends BaseJdbcDao implements TeacherAssignmentDao {

    private static final String SELECT =
            "SELECT id_asignacion, id_profesor, id_materia, `año_academico`, seccion FROM asignaciones_profesor ";

    private static final String SELECT_SLOT =
            "SELECT h.id_horario, h.id_asignacion, h.dia_semana, h.hora_inicio, h.hora_fin FROM horarios_asignacion h ";

    public TeacherAssignmentDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<TeacherAssignment> findById(int id) {
        return queryOne(SELECT + "WHERE id_asignacion = ?", TeacherAssignmentDaoJdbc::map, id);
    }

    @Override
    public List<TeacherAssignment> findAll() {
        return queryList(SELECT + "ORDER BY `año_academico` DESC, id_profesor", TeacherAssignmentDaoJdbc::map);
    }

    @Override
    public int insert(TeacherAssignment a) {
        int id = executeInsert("INSERT INTO asignaciones_profesor (id_profesor, id_materia, `año_academico`, seccion) "
                + "VALUES (?, ?, ?, ?)", a.getTeacherId(), a.getSubjectId(), a.getAcademicYear(), a.getSection());
        a.setId(id);
        return id;
    }

    @Override
    public boolean delete(int assignmentId) {
        return executeUpdate("DELETE FROM asignaciones_profesor WHERE id_asignacion = ?", assignmentId) > 0;
    }

    @Override
    public List<TeacherAssignment> findByTeacher(int teacherId, int academicYear) {
        return queryList(SELECT + "WHERE id_profesor = ? AND `año_academico` = ?",
                TeacherAssignmentDaoJdbc::map, teacherId, academicYear);
    }

    @Override
    public List<TeacherAssignment> findBySubject(int subjectId, int academicYear) {
        return queryList(SELECT + "WHERE id_materia = ? AND `año_academico` = ?",
                TeacherAssignmentDaoJdbc::map, subjectId, academicYear);
    }

    @Override
    public int insertSlot(ScheduleSlot slot) {
        int id = executeInsert("INSERT INTO horarios_asignacion (id_asignacion, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, ?, ?, ?)", slot.getAssignmentId(), slot.getDay(), slot.getStartTime(), slot.getEndTime());
        slot.setId(id);
        return id;
    }

    @Override
    public boolean deleteSlot(int slotId) {
        return executeUpdate("DELETE FROM horarios_asignacion WHERE id_horario = ?", slotId) > 0;
    }

    @Override
    public List<ScheduleSlot> findSlotsByAssignment(int assignmentId) {
        return queryList(SELECT_SLOT + "WHERE h.id_asignacion = ? ORDER BY h.dia_semana, h.hora_inicio",
                TeacherAssignmentDaoJdbc::mapSlot, assignmentId);
    }

    @Override
    public List<ScheduleSlot> findSlotsByTeacher(int teacherId, int academicYear) {
        return queryList(SELECT_SLOT + "JOIN asignaciones_profesor ap ON ap.id_asignacion = h.id_asignacion "
                + "WHERE ap.id_profesor = ? AND ap.`año_academico` = ? ORDER BY h.dia_semana, h.hora_inicio",
                TeacherAssignmentDaoJdbc::mapSlot, teacherId, academicYear);
    }

    private static TeacherAssignment map(ResultSet rs) throws SQLException {
        TeacherAssignment a = new TeacherAssignment();
        a.setId(rs.getInt("id_asignacion"));
        a.setTeacherId(rs.getInt("id_profesor"));
        a.setSubjectId(rs.getInt("id_materia"));
        a.setAcademicYear(rs.getInt("año_academico"));
        a.setSection(rs.getString("seccion"));
        return a;
    }

    private static ScheduleSlot mapSlot(ResultSet rs) throws SQLException {
        ScheduleSlot s = new ScheduleSlot();
        s.setId(rs.getInt("id_horario"));
        s.setAssignmentId(rs.getInt("id_asignacion"));
        s.setDay(EnumUtil.fromLabel(WeekDay.class, rs.getString("dia_semana")));
        s.setStartTime(rs.getObject("hora_inicio", LocalTime.class));
        s.setEndTime(rs.getObject("hora_fin", LocalTime.class));
        return s;
    }
}
