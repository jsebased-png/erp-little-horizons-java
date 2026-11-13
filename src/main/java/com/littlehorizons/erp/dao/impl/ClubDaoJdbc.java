package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.model.Club;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ClubDao (table clubes). */
public class ClubDaoJdbc extends BaseJdbcDao implements ClubDao {

    private static final String SELECT =
            "SELECT id_club, nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, "
            + "esta_activo FROM clubes ";

    public ClubDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Club> findById(int id) {
        return queryOne(SELECT + "WHERE id_club = ?", ClubDaoJdbc::map, id);
    }

    @Override
    public List<Club> findAll() {
        return queryList(SELECT + "ORDER BY nombre_club", ClubDaoJdbc::map);
    }

    @Override
    public int insert(Club c) {
        int id = executeInsert("INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, "
                + "capacidad_maxima, horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)", values(c));
        c.setId(id);
        return id;
    }

    @Override
    public boolean update(Club c) {
        return executeUpdate("UPDATE clubes SET nombre_club = ?, descripcion = ?, id_profesor_supervisor = ?, "
                + "capacidad_maxima = ?, horario_reunion = ?, esta_activo = ? WHERE id_club = ?",
                append(values(c), c.getId())) > 0;
    }

    @Override
    public boolean setActive(int clubId, boolean active) {
        return executeUpdate("UPDATE clubes SET esta_activo = ? WHERE id_club = ?", active, clubId) > 0;
    }

    @Override
    public Optional<Club> findByName(String name) {
        return queryOne(SELECT + "WHERE nombre_club = ?", ClubDaoJdbc::map, name);
    }

    private static Object[] values(Club c) {
        return new Object[] {
            c.getName(), c.getDescription(), c.getSupervisorId(), c.getMaxCapacity(), c.getMeetingSchedule(), c.isActive()
        };
    }

    private static Club map(ResultSet rs) throws SQLException {
        Club c = new Club();
        c.setId(rs.getInt("id_club"));
        c.setName(rs.getString("nombre_club"));
        c.setDescription(rs.getString("descripcion"));
        c.setSupervisorId(nullableInt(rs, "id_profesor_supervisor"));
        c.setMaxCapacity(rs.getInt("capacidad_maxima"));
        c.setMeetingSchedule(rs.getString("horario_reunion"));
        c.setActive(rs.getBoolean("esta_activo"));
        return c;
    }
}
