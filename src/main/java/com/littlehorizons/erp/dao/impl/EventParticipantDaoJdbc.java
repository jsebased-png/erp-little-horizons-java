package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventPosition;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of EventParticipantDao (table participantes_evento). */
public class EventParticipantDaoJdbc extends BaseJdbcDao implements EventParticipantDao {

    private static final String SELECT =
            "SELECT id_participacion, id_evento, id_estudiante, rol_participacion, posicion_obtenida, observaciones "
            + "FROM participantes_evento ";

    public EventParticipantDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<EventParticipant> findById(int id) {
        return queryOne(SELECT + "WHERE id_participacion = ?", EventParticipantDaoJdbc::map, id);
    }

    @Override
    public List<EventParticipant> findAll() {
        return queryList(SELECT + "ORDER BY id_evento, id_estudiante", EventParticipantDaoJdbc::map);
    }

    @Override
    public int insert(EventParticipant p) {
        int id = executeInsert("INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, "
                + "posicion_obtenida, observaciones) VALUES (?, ?, ?, ?, ?)",
                p.getEventId(), p.getStudentId(), p.getRole(), p.getPosition(), p.getObservations());
        p.setId(id);
        return id;
    }

    @Override
    public boolean update(EventParticipant p) {
        return executeUpdate("UPDATE participantes_evento SET rol_participacion = ?, posicion_obtenida = ?, "
                + "observaciones = ? WHERE id_participacion = ?",
                p.getRole(), p.getPosition(), p.getObservations(), p.getId()) > 0;
    }

    @Override
    public boolean delete(int participationId) {
        return executeUpdate("DELETE FROM participantes_evento WHERE id_participacion = ?", participationId) > 0;
    }

    @Override
    public boolean exists(int eventId, int studentId) {
        return scalarInt("SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? AND id_estudiante = ?",
                eventId, studentId) > 0;
    }

    @Override
    public List<EventParticipant> findByEvent(int eventId) {
        return queryList(SELECT + "WHERE id_evento = ? ORDER BY id_estudiante", EventParticipantDaoJdbc::map, eventId);
    }

    @Override
    public List<EventParticipant> findByStudent(int studentId) {
        return queryList(SELECT + "WHERE id_estudiante = ? ORDER BY id_evento", EventParticipantDaoJdbc::map, studentId);
    }

    private static EventParticipant map(ResultSet rs) throws SQLException {
        EventParticipant p = new EventParticipant();
        p.setId(rs.getInt("id_participacion"));
        p.setEventId(rs.getInt("id_evento"));
        p.setStudentId(rs.getInt("id_estudiante"));
        p.setRole(EnumUtil.fromLabel(ParticipationRole.class, rs.getString("rol_participacion")));
        p.setPosition(EnumUtil.fromLabel(EventPosition.class, rs.getString("posicion_obtenida")));
        p.setObservations(rs.getString("observaciones"));
        return p;
    }
}
