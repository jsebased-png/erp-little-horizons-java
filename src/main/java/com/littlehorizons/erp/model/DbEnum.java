package com.littlehorizons.erp.model;

/**
 * Contract for enums that map to a MySQL ENUM text value.
 * It exists so the DAO layer can save and read any enum with the same code.
 */
public interface DbEnum {

    /** Exact text stored in the database (for example "Excusa Médica"). */
    String getLabel();
}
