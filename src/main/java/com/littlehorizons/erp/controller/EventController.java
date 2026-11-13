package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventPosition;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.Validator;

import java.util.List;

/**
 * Business rules for school events and their participants.
 * Rule: the final position (1er, 2do, 3er Lugar, Participante) can only be saved in events with competition.
 * The database stores the position as free text, so this controller is the one that guarantees valid values.
 */
// SOLID: SRP – event rules only
public class EventController {

    private final EventDao eventDao;
    private final EventParticipantDao participantDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public EventController(EventDao eventDao, EventParticipantDao participantDao, StudentDao studentDao,
                           TeacherDao teacherDao) {
        this.eventDao = eventDao;
        this.participantDao = participantDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ------------------------------------------------------------------ events

    public SchoolEvent createEvent(SchoolEvent event) {
        normalize(event);
        event.setActive(true);
        event.setId(eventDao.insert(event));
        return event;
    }

    public void updateEvent(SchoolEvent event) {
        findEvent(event.getId());
        normalize(event);
        eventDao.update(event);
    }

    public void setActive(int eventId, boolean active) {
        findEvent(eventId);
        eventDao.setActive(eventId, active);
    }

    public SchoolEvent findEvent(int eventId) {
        return eventDao.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found: id " + eventId));
    }

    public List<SchoolEvent> listEvents() {
        return eventDao.findAll();
    }

    public List<SchoolEvent> listEventsByType(EventType type) {
        return eventDao.findByType(Validator.requireNotNull("Event type", type));
    }

    // ------------------------------------------------------------------ participants

    /** Adds a student to an event. In competitive events the default role is Competidor. */
    public EventParticipant addParticipant(int eventId, int studentId, ParticipationRole role, String observations) {
        SchoolEvent event = findEvent(eventId);
        if (!event.isActive()) {
            throw new BusinessRuleException("The event is not active.");
        }
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: id " + studentId));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE students can participate in events.");
        }
        if (participantDao.exists(eventId, studentId)) {
            throw new BusinessRuleException("The student is already registered in this event.");
        }

        EventParticipant participant = new EventParticipant();
        participant.setEventId(eventId);
        participant.setStudentId(studentId);
        participant.setRole(role != null ? role
                : (event.isCompetitive() ? ParticipationRole.COMPETITOR : ParticipationRole.PARTICIPANT));
        participant.setObservations(Validator.optionalText("Observations", observations, 500));
        participant.setId(participantDao.insert(participant));
        return participant;
    }

    /** Saves the final position of a participant. Only allowed when the event has competition. */
    public void recordPosition(int participantId, EventPosition position) {
        Validator.requireNotNull("Position", position);
        EventParticipant participant = participantDao.findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant not found: id " + participantId));
        SchoolEvent event = findEvent(participant.getEventId());
        if (!event.isCompetitive()) {
            throw new BusinessRuleException("The event \"" + event.getName() + "\" has no competition; positions are not allowed.");
        }
        participant.setPosition(position);
        participantDao.update(participant);
    }

    public void removeParticipant(int participantId) {
        if (!participantDao.delete(participantId)) {
            throw new NotFoundException("Participant not found: id " + participantId);
        }
    }

    public List<EventParticipant> listParticipants(int eventId) {
        return participantDao.findByEvent(eventId);
    }

    public List<EventParticipant> listParticipationsOfStudent(int studentId) {
        return participantDao.findByStudent(studentId);
    }

    // ------------------------------------------------------------------ private helpers

    private void normalize(SchoolEvent e) {
        e.setName(Validator.requireText("Event name", e.getName(), 150));
        e.setDescription(Validator.optionalText("Description", e.getDescription(), 1000));
        Validator.requireNotNull("Event type", e.getType());
        Validator.requireNotNull("Event date", e.getEventDate());
        e.setPlace(Validator.optionalText("Place", e.getPlace(), 150));
        if (e.getOrganizerId() != null) {
            teacherDao.findById(e.getOrganizerId())
                    .orElseThrow(() -> new NotFoundException("Organizer teacher not found: id " + e.getOrganizerId()));
        }
    }
}
