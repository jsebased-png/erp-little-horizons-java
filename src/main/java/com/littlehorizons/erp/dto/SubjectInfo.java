package com.littlehorizons.erp.dto;

/**
 * Identity of a subject (materias: id_materia, codigo_materia, nombre_materia).
 * Being a record it has value-based equals/hashCode, so it can be used directly as the key of Collectors.groupingBy.
 */
public record SubjectInfo(int id, String code, String name) {
}
