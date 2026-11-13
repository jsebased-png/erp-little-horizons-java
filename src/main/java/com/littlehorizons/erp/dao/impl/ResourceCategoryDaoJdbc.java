package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.ResourceCategoryDao;
import com.littlehorizons.erp.model.ResourceCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ResourceCategoryDao (table categorias_recurso). */
public class ResourceCategoryDaoJdbc extends BaseJdbcDao implements ResourceCategoryDao {

    private static final String SELECT = "SELECT id_categoria, nombre_categoria, descripcion FROM categorias_recurso ";

    public ResourceCategoryDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<ResourceCategory> findById(int id) {
        return queryOne(SELECT + "WHERE id_categoria = ?", ResourceCategoryDaoJdbc::map, id);
    }

    @Override
    public List<ResourceCategory> findAll() {
        return queryList(SELECT + "ORDER BY nombre_categoria", ResourceCategoryDaoJdbc::map);
    }

    private static ResourceCategory map(ResultSet rs) throws SQLException {
        ResourceCategory c = new ResourceCategory();
        c.setId(rs.getInt("id_categoria"));
        c.setName(rs.getString("nombre_categoria"));
        c.setDescription(rs.getString("descripcion"));
        return c;
    }
}
