package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ClubMembershipDao (table membresias_club). */
public class ClubMembershipDaoJdbc extends BaseJdbcDao implements ClubMembershipDao {

    private static final String SELECT =
            "SELECT id_membresia, id_estudiante, id_club, fecha_ingreso, estado_membresia, fecha_retiro, "
            + "motivo_retiro FROM membresias_club ";

    public ClubMembershipDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<ClubMembership> findById(int id) {
        return queryOne(SELECT + "WHERE id_membresia = ?", ClubMembershipDaoJdbc::map, id);
    }

    @Override
    public List<ClubMembership> findAll() {
        return queryList(SELECT + "ORDER BY id_club, fecha_ingreso", ClubMembershipDaoJdbc::map);
    }

    @Override
    public int insert(ClubMembership m) {
        int id = executeInsert("INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, "
                + "estado_membresia) VALUES (?, ?, ?, ?)",
                m.getStudentId(), m.getClubId(), m.getJoinDate(), m.getStatus());
        m.setId(id);
        return id;
    }

    @Override
    public boolean withdraw(int membershipId, LocalDate leaveDate, String reason) {
        return executeUpdate("UPDATE membresias_club SET estado_membresia = ?, fecha_retiro = ?, motivo_retiro = ? "
                + "WHERE id_membresia = ?", EnrollmentStatus.WITHDRAWN, leaveDate, reason, membershipId) > 0;
    }

    @Override
    public List<ClubMembership> findByClub(int clubId) {
        return queryList(SELECT + "WHERE id_club = ? ORDER BY fecha_ingreso", ClubMembershipDaoJdbc::map, clubId);
    }

    @Override
    public List<ClubMembership> findByStudent(int studentId) {
        return queryList(SELECT + "WHERE id_estudiante = ? ORDER BY fecha_ingreso DESC",
                ClubMembershipDaoJdbc::map, studentId);
    }

    @Override
    public int countActiveByClub(int clubId) {
        return scalarInt("SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = ?",
                clubId, EnrollmentStatus.ACTIVE);
    }

    @Override
    public boolean existsActive(int studentId, int clubId) {
        return scalarInt("SELECT COUNT(*) FROM membresias_club WHERE id_estudiante = ? AND id_club = ? "
                + "AND estado_membresia = ?", studentId, clubId, EnrollmentStatus.ACTIVE) > 0;
    }

    private static ClubMembership map(ResultSet rs) throws SQLException {
        ClubMembership m = new ClubMembership();
        m.setId(rs.getInt("id_membresia"));
        m.setStudentId(rs.getInt("id_estudiante"));
        m.setClubId(rs.getInt("id_club"));
        m.setJoinDate(date(rs, "fecha_ingreso"));
        m.setStatus(EnumUtil.fromLabel(EnrollmentStatus.class, rs.getString("estado_membresia")));
        m.setLeaveDate(date(rs, "fecha_retiro"));
        m.setLeaveReason(rs.getString("motivo_retiro"));
        return m;
    }
}
