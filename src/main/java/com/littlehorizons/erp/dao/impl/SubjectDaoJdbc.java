package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.model.Subject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of SubjectDao (table materias). */
public class SubjectDaoJdbc extends BaseJdbcDao implements SubjectDao {

    private static final String SELECT =
            "SELECT id_materia, codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa "
            + "FROM materias ";

    public SubjectDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Subject> findById(int id) {
        return queryOne(SELECT + "WHERE id_materia = ?", SubjectDaoJdbc::map, id);
    }

    @Override
    public List<Subject> findAll() {
        return queryList(SELECT + "ORDER BY id_grado, nombre_materia", SubjectDaoJdbc::map);
    }

    @Override
    public int insert(Subject s) {
        int id = executeInsert("INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, "
                + "horas_semanales, esta_activa) VALUES (?, ?, ?, ?, ?, ?)", values(s));
        s.setId(id);
        return id;
    }

    @Override
    public boolean update(Subject s) {
        return executeUpdate("UPDATE materias SET codigo_materia = ?, nombre_materia = ?, descripcion = ?, "
                + "id_grado = ?, horas_semanales = ?, esta_activa = ? WHERE id_materia = ?",
                append(values(s), s.getId())) > 0;
    }

    @Override
    public boolean setActive(int subjectId, boolean active) {
        return executeUpdate("UPDATE materias SET esta_activa = ? WHERE id_materia = ?", active, subjectId) > 0;
    }

    @Override
    public Optional<Subject> findByCode(String code) {
        return queryOne(SELECT + "WHERE codigo_materia = ?", SubjectDaoJdbc::map, code);
    }

    @Override
    public List<Subject> findByGrade(int gradeId) {
        return queryList(SELECT + "WHERE id_grado = ? ORDER BY nombre_materia", SubjectDaoJdbc::map, gradeId);
    }

    private static Object[] values(Subject s) {
        return new Object[] {
            s.getCode(), s.getName(), s.getDescription(), s.getGradeId(), s.getWeeklyHours(), s.isActive()
        };
    }

    private static Subject map(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setId(rs.getInt("id_materia"));
        s.setCode(rs.getString("codigo_materia"));
        s.setName(rs.getString("nombre_materia"));
        s.setDescription(rs.getString("descripcion"));
        s.setGradeId(rs.getInt("id_grado"));
        s.setWeeklyHours(rs.getInt("horas_semanales"));
        s.setActive(rs.getBoolean("esta_activa"));
        return s;
    }
}
