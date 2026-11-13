package com.littlehorizons.erp.model;

/** Identity document types. */
public enum DocumentType implements DbEnum {
    TI("TI"),
    CC("CC"),
    CE("CE"),
    PASSPORT("Pasaporte");

    private final String label;

    DocumentType(String label) {
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
