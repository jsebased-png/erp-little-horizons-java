package com.littlehorizons.erp.model;

/** Role of a student inside an event. */
public enum ParticipationRole implements DbEnum {
    PARTICIPANT("Participante"),
    COMPETITOR("Competidor"),
    ORGANIZER("Organizador"),
    ATTENDEE("Asistente");

    private final String label;

    ParticipationRole(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /** Views print the same text the database stores. */
    @Override
    public String toString() {
        return label;
    }
}
