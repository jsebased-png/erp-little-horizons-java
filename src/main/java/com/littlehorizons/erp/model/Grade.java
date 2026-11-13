package com.littlehorizons.erp.model;

/**
 * Plain data holder (POJO) for table(s): grados.
 * It only carries data between layers and has no business logic.
 */
public class Grade {

    private int id;
    private String name;
    private int numericLevel;

    public Grade() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumericLevel() {
        return numericLevel;
    }

    public void setNumericLevel(int numericLevel) {
        this.numericLevel = numericLevel;
    }
}
