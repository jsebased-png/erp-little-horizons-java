package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for grade enrollment (matrícula) and subject enrollment (inscripción en materias).
 * Rules: one grade enrollment per student per year; a student can only take subjects of his/her own grade.
 */
// SOLID: SRP – enrollment rules only
public class EnrollmentController {

    private final EnrollmentDao enrollmentDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;

    public EnrollmentController(EnrollmentDao enrollmentDao, SubjectEnrollmentDao subjectEnrollmentDao,
                                StudentDao studentDao, SubjectDao subjectDao) {
        this.enrollmentDao = enrollmentDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
    }

    // ------------------------------------------------------------------ grade enrollment

    public Enrollment enrollInGrade(int studentId, int gradeId, int academicYear, String notes) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: id " + studentId));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE students can be enrolled (current status: " + student.getStatus() + ").");
        }
        int currentYear = LocalDate.now().getYear();
        if (academicYear < currentYear - 1 || academicYear > currentYear + 1) {
            throw new BusinessRuleException("Academic year must be between " + (currentYear - 1) + " and " + (currentYear + 1) + ".");
        }
        enrollmentDao.findByStudentAndYear(studentId, academicYear).ifPresent(existing -> {
            throw new BusinessRuleException("The student already has an enrollment for " + academicYear + ".");
        });

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setGradeId(gradeId);
        enrollment.setAcademicYear(academicYear);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setNotes(Validator.optionalText("Notes", notes, 1000));
        enrollment.setId(enrollmentDao.insert(enrollment));
        return enrollment;
    }

    public void withdrawFromGrade(int enrollmentId) {
        Enrollment enrollment = enrollmentDao.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment not found: id " + enrollmentId));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE enrollments can be withdrawn.");
        }
        enrollmentDao.updateStatus(enrollmentId, EnrollmentStatus.WITHDRAWN, LocalDate.now());
    }

    public List<Enrollment> listEnrollmentsByStudent(int studentId) {
        return enrollmentDao.findByStudent(studentId);
    }

    public List<Enrollment> listEnrollmentsByYear(int academicYear) {
        return enrollmentDao.findByYear(academicYear);
    }

    /** The ACTIVE grade enrollment of a student in a year, if any. */
    public Optional<Enrollment> findActiveEnrollment(int studentId, int academicYear) {
        return enrollmentDao.findByStudentAndYear(studentId, academicYear)
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE);
    }

    // ------------------------------------------------------------------ subject enrollment

    public SubjectEnrollment enrollInSubject(int studentId, int subjectId, int academicYear,
                                             AcademicPeriod period, String section) {
        Validator.requireNotNull("Period", period);
        if (period == AcademicPeriod.FINAL || period == AcademicPeriod.ANNUAL) {
            throw new BusinessRuleException("Subjects are enrolled by period I, II, III or IV.");
        }
        Enrollment gradeEnrollment = findActiveEnrollment(studentId, academicYear)
                .orElseThrow(() -> new BusinessRuleException("The student has no ACTIVE grade enrollment in " + academicYear + "."));
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Subject not found: id " + subjectId));
        if (!subject.isActive()) {
            throw new BusinessRuleException("The subject is not active.");
        }
        // Core rule of the module: the subject must belong to the grade where the student is enrolled
        if (subject.getGradeId() != gradeEnrollment.getGradeId()) {
            throw new BusinessRuleException("That subject does not belong to the student's grade.");
        }
        if (subjectEnrollmentDao.exists(studentId, subjectId, academicYear, period)) {
            throw new BusinessRuleException("The student is already enrolled in that subject for period " + period + ".");
        }

        SubjectEnrollment enrollment = new SubjectEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setSubjectId(subjectId);
        enrollment.setAcademicYear(academicYear);
        enrollment.setPeriod(period);
        enrollment.setSection(Validator.optionalText("Section", section, 10));
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setId(subjectEnrollmentDao.insert(enrollment));
        return enrollment;
    }

    /** Enrolls the student in ALL the subjects of his/her grade for one period; returns how many were created. */
    public int enrollInAllGradeSubjects(int studentId, int academicYear, AcademicPeriod period) {
        Enrollment gradeEnrollment = findActiveEnrollment(studentId, academicYear)
                .orElseThrow(() -> new BusinessRuleException("The student has no ACTIVE grade enrollment in " + academicYear + "."));
        // Stream: active subjects of the grade that the student does not have yet in this period
        List<Subject> pending = subjectDao.findByGrade(gradeEnrollment.getGradeId()).stream()
                .filter(Subject::isActive)
                .filter(s -> !subjectEnrollmentDao.exists(studentId, s.getId(), academicYear, period))
                .toList();
        pending.forEach(s -> enrollInSubject(studentId, s.getId(), academicYear, period, null));
        return pending.size();
    }

    public void withdrawFromSubject(int subjectEnrollmentId) {
        subjectEnrollmentDao.findById(subjectEnrollmentId)
                .orElseThrow(() -> new NotFoundException("Subject enrollment not found: id " + subjectEnrollmentId));
        subjectEnrollmentDao.updateStatus(subjectEnrollmentId, EnrollmentStatus.WITHDRAWN);
    }

    public List<SubjectEnrollment> listSubjectEnrollments(int studentId, int academicYear) {
        return subjectEnrollmentDao.findByStudentAndYear(studentId, academicYear);
    }
}
