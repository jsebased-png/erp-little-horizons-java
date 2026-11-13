package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventParticipant;

import java.util.List;

/** Data access contract for event participants and their final position. */
public interface EventParticipantDao extends ReadableDao<EventParticipant> {

    int insert(EventParticipant participant);

    /** Updates role, position and observations. */
    boolean update(EventParticipant participant);

    boolean delete(int participationId);

    boolean exists(int eventId, int studentId);

    List<EventParticipant> findByEvent(int eventId);

    List<EventParticipant> findByStudent(int studentId);
}
