package com.littlehorizons.erp.model;

/** Status of a resource loan. */
public enum LoanStatus implements DbEnum {
    ACTIVE("Activo"),
    RETURNED("Devuelto"),
    OVERDUE("Vencido");

    private final String label;

    LoanStatus(String label) {
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
