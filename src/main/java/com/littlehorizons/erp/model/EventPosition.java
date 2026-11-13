package com.littlehorizons.erp.model;

/** Final position in a competitive event. */
public enum EventPosition implements DbEnum {
    FIRST("1er Lugar"),
    SECOND("2do Lugar"),
    THIRD("3er Lugar"),
    PARTICIPANT("Participante");

    private final String label;

    EventPosition(String label) {
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
