package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.util.EnumUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ResourceDao (table recursos). */
public class ResourceDaoJdbc extends BaseJdbcDao implements ResourceDao {

    private static final String SELECT =
            "SELECT id_recurso, codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion, "
            + "ubicacion_fisica, estado_recurso FROM recursos ";

    public ResourceDaoJdbc(DatabaseConnection database) {
        super(database);
    }

    @Override
    public Optional<Resource> findById(int id) {
        return queryOne(SELECT + "WHERE id_recurso = ?", ResourceDaoJdbc::map, id);
    }

    @Override
    public List<Resource> findAll() {
        return queryList(SELECT + "ORDER BY codigo_recurso", ResourceDaoJdbc::map);
    }

    @Override
    public int insert(Resource r) {
        int id = executeInsert("INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, "
                + "fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)", values(r));
        r.setId(id);
        return id;
    }

    @Override
    public boolean update(Resource r) {
        return executeUpdate("UPDATE recursos SET codigo_recurso = ?, nombre_recurso = ?, id_categoria = ?, "
                + "descripcion = ?, fecha_adquisicion = ?, ubicacion_fisica = ?, estado_recurso = ? "
                + "WHERE id_recurso = ?", append(values(r), r.getId())) > 0;
    }

    @Override
    public boolean updateStatus(int resourceId, ResourceStatus status) {
        return executeUpdate("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", status, resourceId) > 0;
    }

    @Override
    public Optional<Resource> findByCode(String code) {
        return queryOne(SELECT + "WHERE codigo_recurso = ?", ResourceDaoJdbc::map, code);
    }

    @Override
    public List<Resource> findByStatus(ResourceStatus status) {
        return queryList(SELECT + "WHERE estado_recurso = ? ORDER BY codigo_recurso", ResourceDaoJdbc::map, status);
    }

    @Override
    public List<Resource> findByCategory(int categoryId) {
        return queryList(SELECT + "WHERE id_categoria = ? ORDER BY codigo_recurso", ResourceDaoJdbc::map, categoryId);
    }

    private static Object[] values(Resource r) {
        return new Object[] {
            r.getCode(), r.getName(), r.getCategoryId(), r.getDescription(), r.getAcquisitionDate(),
            r.getLocation(), r.getStatus()
        };
    }

    private static Resource map(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setId(rs.getInt("id_recurso"));
        r.setCode(rs.getString("codigo_recurso"));
        r.setName(rs.getString("nombre_recurso"));
        r.setCategoryId(rs.getInt("id_categoria"));
        r.setDescription(rs.getString("descripcion"));
        r.setAcquisitionDate(date(rs, "fecha_adquisicion"));
        r.setLocation(rs.getString("ubicacion_fisica"));
        r.setStatus(EnumUtil.fromLabel(ResourceStatus.class, rs.getString("estado_recurso")));
        return r;
    }
}
