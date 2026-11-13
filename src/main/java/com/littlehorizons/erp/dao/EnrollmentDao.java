package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access contract for grade enrollments (table matriculas). */
public interface EnrollmentDao extends ReadableDao<Enrollment> {

    int insert(Enrollment enrollment);

    boolean updateStatus(int enrollmentId, EnrollmentStatus status, LocalDate withdrawalDate);

    List<Enrollment> findByStudent(int studentId);

    List<Enrollment> findByYear(int academicYear);

    Optional<Enrollment> findByStudentAndYear(int studentId, int academicYear);
}
