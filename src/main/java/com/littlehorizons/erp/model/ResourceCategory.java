package com.littlehorizons.erp.model;

/**
 * Plain data holder (POJO) for table(s): categorias_recurso.
 * It only carries data between layers and has no business logic.
 */
public class ResourceCategory {

    private int id;
    private String name;
    private String description;

    public ResourceCategory() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
