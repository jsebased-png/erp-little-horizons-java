package com.littlehorizons.erp.model;

/** Availability of a school resource. */
public enum ResourceStatus implements DbEnum {
    AVAILABLE("Disponible"),
    LOANED("Prestado"),
    MAINTENANCE("En Mantenimiento"),
    DISCARDED("Dado de Baja");

    private final String label;

    ResourceStatus(String label) {
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
