package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceStatus;

import java.util.List;
import java.util.Optional;

/** Data access contract for the resource catalog. */
public interface ResourceDao extends ReadableDao<Resource> {

    int insert(Resource resource);

    boolean update(Resource resource);

    boolean updateStatus(int resourceId, ResourceStatus status);

    Optional<Resource> findByCode(String code);

    List<Resource> findByStatus(ResourceStatus status);

    List<Resource> findByCategory(int categoryId);
}
