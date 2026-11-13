package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.SubjectEnrollment;

import java.util.List;

/** Data access contract for student enrollments in subjects. */
public interface SubjectEnrollmentDao extends ReadableDao<SubjectEnrollment> {

    int insert(SubjectEnrollment enrollment);

    boolean updateStatus(int enrollmentId, EnrollmentStatus status);

    boolean exists(int studentId, int subjectId, int academicYear, AcademicPeriod period);

    List<SubjectEnrollment> findByStudentAndYear(int studentId, int academicYear);

    List<SubjectEnrollment> findBySubjectAndPeriod(int subjectId, int academicYear, AcademicPeriod period);
}
