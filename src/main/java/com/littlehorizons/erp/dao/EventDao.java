package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.SchoolEvent;

import java.util.List;

/** Data access contract for school events. */
public interface EventDao extends ReadableDao<SchoolEvent> {

    int insert(SchoolEvent event);

    boolean update(SchoolEvent event);

    boolean setActive(int eventId, boolean active);

    List<SchoolEvent> findByType(EventType type);
}
