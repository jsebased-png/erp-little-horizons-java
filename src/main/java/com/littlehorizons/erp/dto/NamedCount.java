package com.littlehorizons.erp.dto;

/** A label with a counter, used by participation reports (club name and members, student and medals...). */
public record NamedCount(String name, long count) {
}
