package com.littlehorizons.erp.model;

/** Who borrows a resource (each type has its own table). */
public enum BorrowerType implements DbEnum {
    STUDENT("Estudiante"),
    TEACHER("Profesor");

    private final String label;

    BorrowerType(String label) {
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
