package com.littlehorizons.erp.model;

/** Status of a grade enrollment, subject enrollment or club membership. */
public enum EnrollmentStatus implements DbEnum {
    ACTIVE("Activa"),
    COMPLETED("Completada"),
    WITHDRAWN("Retirada");

    private final String label;

    EnrollmentStatus(String label) {
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
