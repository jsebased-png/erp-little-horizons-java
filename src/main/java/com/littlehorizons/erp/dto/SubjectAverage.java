package com.littlehorizons.erp.dto;

import java.math.BigDecimal;

/** General average of one subject, already rounded to 1 decimal (HALF_UP). */
public record SubjectAverage(SubjectInfo subject, BigDecimal average) {
}
