package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.model.Grade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of GradeDao (table grados). */
public class GradeDaoJdbc extends BaseJdbcDao implements GradeDao {

    private static final String SELECT = "SELECT id_grado, nombre_grado, nivel_numerico FROM grados ";

    public GradeDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Grade> findById(int id) {
        return queryOne(SELECT + "WHERE id_grado = ?", GradeDaoJdbc::map, id);
    }

    @Override
    public List<Grade> findAll() {
        return queryList(SELECT + "ORDER BY nivel_numerico", GradeDaoJdbc::map);
    }

    private static Grade map(ResultSet rs) throws SQLException {
        Grade grade = new Grade();
        grade.setId(rs.getInt("id_grado"));
        grade.setName(rs.getString("nombre_grado"));
        grade.setNumericLevel(rs.getInt("nivel_numerico"));
        return grade;
    }
}
