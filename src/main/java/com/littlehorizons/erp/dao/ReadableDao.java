package com.littlehorizons.erp.dao;

import java.util.List;
import java.util.Optional;

/**
 * Smallest contract shared by every DAO: read one record or read all.
 * It exists so write operations are declared only in the DAOs that really need them.
 */
// SOLID: ISP – read-only catalogs (grades, categories) depend on reads only, no write methods forced on them
public interface ReadableDao<T> {

    Optional<T> findById(int id);

    List<T> findAll();
}
