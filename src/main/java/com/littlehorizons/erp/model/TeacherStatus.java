package com.littlehorizons.erp.model;

/** Status of a teacher. */
public enum TeacherStatus implements DbEnum {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    ON_LEAVE("Licencia");

    private final String label;

    TeacherStatus(String label) {
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
