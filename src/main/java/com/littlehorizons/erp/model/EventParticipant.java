package com.littlehorizons.erp.model;

/**
 * Plain data holder (POJO) for table(s): participantes_evento.
 * It only carries data between layers and has no business logic.
 */
public class EventParticipant {

    private int id;
    private int eventId;
    private int studentId;
    private ParticipationRole role;
    private EventPosition position;
    private String observations;

    public EventParticipant() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public ParticipationRole getRole() {
        return role;
    }

    public void setRole(ParticipationRole role) {
        this.role = role;
    }

    public EventPosition getPosition() {
        return position;
    }

    public void setPosition(EventPosition position) {
        this.position = position;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
