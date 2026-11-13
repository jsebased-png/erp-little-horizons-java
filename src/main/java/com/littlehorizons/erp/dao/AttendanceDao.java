package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access contract for daily attendance. */
public interface AttendanceDao extends ReadableDao<Attendance> {

    int insert(Attendance attendance);

    boolean update(Attendance attendance);

    Optional<Attendance> find(int studentId, int subjectId, LocalDate date);

    List<Attendance> findByStudent(int studentId);

    /** Attendance between two dates (both included), used by the weekly absence report. */
    List<Attendance> findBetween(LocalDate from, LocalDate to);
}
