package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;

import java.util.List;
import java.util.Optional;

/** Data access contract for guardians and the student-guardian link table. */
public interface GuardianDao extends ReadableDao<Guardian> {

    int insert(Guardian guardian);

    boolean update(Guardian guardian);

    Optional<Guardian> findByDocument(String documentNumber);

    /** Links a guardian to a student (many-to-many). */
    boolean link(int studentId, int guardianId, boolean primary, boolean emergencyContact);

    boolean unlink(int studentId, int guardianId);

    /** Guardians of one student, with primaryContact / emergencyContact filled from the link. */
    List<Guardian> findByStudentId(int studentId);

    /** Guardian in charge of payments for a student (primary contact first). */
    Optional<Guardian> findFinancialGuardian(int studentId);

    int countByStudent(int studentId);

    /** How many ACTIVE students share this guardian (used for the siblings discount). */
    int countActiveStudents(int guardianId);
}
