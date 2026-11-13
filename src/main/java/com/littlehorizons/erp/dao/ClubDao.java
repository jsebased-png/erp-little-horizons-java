package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;

import java.util.Optional;

/** Data access contract for clubs. */
public interface ClubDao extends ReadableDao<Club> {

    int insert(Club club);

    boolean update(Club club);

    boolean setActive(int clubId, boolean active);

    Optional<Club> findByName(String name);
}
