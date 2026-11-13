package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of EventDao (table eventos). */
public class EventDaoJdbc extends BaseJdbcDao implements EventDao {

    private static final String SELECT =
            "SELECT id_evento, nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, tiene_competencia, "
            + "id_organizador, esta_activo FROM eventos ";

    public EventDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<SchoolEvent> findById(int id) {
        return queryOne(SELECT + "WHERE id_evento = ?", EventDaoJdbc::map, id);
    }

    @Override
    public List<SchoolEvent> findAll() {
        return queryList(SELECT + "ORDER BY fecha_evento DESC", EventDaoJdbc::map);
    }

    @Override
    public int insert(SchoolEvent e) {
        int id = executeInsert("INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, "
                + "tiene_competencia, id_organizador, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", values(e));
        e.setId(id);
        return id;
    }

    @Override
    public boolean update(SchoolEvent e) {
        return executeUpdate("UPDATE eventos SET nombre_evento = ?, descripcion = ?, tipo_evento = ?, "
                + "fecha_evento = ?, lugar = ?, tiene_competencia = ?, id_organizador = ?, esta_activo = ? "
                + "WHERE id_evento = ?", append(values(e), e.getId())) > 0;
    }

    @Override
    public boolean setActive(int eventId, boolean active) {
        return executeUpdate("UPDATE eventos SET esta_activo = ? WHERE id_evento = ?", active, eventId) > 0;
    }

    @Override
    public List<SchoolEvent> findByType(EventType type) {
        return queryList(SELECT + "WHERE tipo_evento = ? ORDER BY fecha_evento DESC", EventDaoJdbc::map, type);
    }

    private static Object[] values(SchoolEvent e) {
        return new Object[] {
            e.getName(), e.getDescription(), e.getType(), e.getEventDate(), e.getPlace(), e.isCompetitive(),
            e.getOrganizerId(), e.isActive()
        };
    }

    private static SchoolEvent map(ResultSet rs) throws SQLException {
        SchoolEvent e = new SchoolEvent();
        e.setId(rs.getInt("id_evento"));
        e.setName(rs.getString("nombre_evento"));
        e.setDescription(rs.getString("descripcion"));
        e.setType(EnumUtil.fromLabel(EventType.class, rs.getString("tipo_evento")));
        e.setEventDate(date(rs, "fecha_evento"));
        e.setPlace(rs.getString("lugar"));
        e.setCompetitive(rs.getBoolean("tiene_competencia"));
        e.setOrganizerId(nullableInt(rs, "id_organizador"));
        e.setActive(rs.getBoolean("esta_activo"));
        return e;
    }
}
