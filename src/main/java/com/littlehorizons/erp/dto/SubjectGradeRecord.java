package com.littlehorizons.erp.dto;

import com.littlehorizons.erp.model.AcademicPeriod;

import java.math.BigDecimal;

/**
 * One row of the JOIN registros_academicos + materias: a grade together with the subject it belongs to.
 * It is a DTO (not AcademicRecord) because the report needs the subject code and name, which the plain model
 * does not carry; this way the controller needs no extra query per grade (no N+1).
 */
public record SubjectGradeRecord(int recordId, BigDecimal score, AcademicPeriod period, SubjectInfo subject) {
}
