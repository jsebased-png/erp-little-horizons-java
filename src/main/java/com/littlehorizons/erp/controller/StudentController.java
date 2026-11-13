package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.List;

/**
 * Business rules for students and their guardians.
 * It exists so the view never talks to the DAOs: it asks this class, which validates and applies the rules.
 * Rules: a student always has at least one guardian; "delete" is a soft delete (status Retirado).
 */
// SOLID: SRP – student and guardian rules only (grades, payments, clubs live in other controllers)
// SOLID: DIP – depends on DAO interfaces received by constructor, never on JDBC classes
public class StudentController {

    private static final int MIN_AGE = 2;
    private static final int MAX_AGE = 25;

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final AppLogger logger = AppLogger.getInstance();

    public StudentController(StudentDao studentDao, GuardianDao guardianDao, EnrollmentDao enrollmentDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
    }

    // ------------------------------------------------------------------ students

    /**
     * Registers a student together with the first guardian (primary contact and emergency contact).
     * If the guardian document already exists (for example a brother is already registered) that guardian is reused.
     */
    public Student registerStudent(Student student, Guardian guardian) {
        normalizeStudent(student);
        normalizeGuardian(guardian);

        if (studentDao.findByDocument(student.getDocumentNumber()).isPresent()) {
            throw new BusinessRuleException("A student with document " + student.getDocumentNumber() + " already exists.");
        }
        int guardianId = resolveGuardianId(guardian);

        student.setStatus(StudentStatus.ACTIVE);
        if (student.getAdmissionDate() == null) {
            student.setAdmissionDate(LocalDate.now());
        }
        student.setId(studentDao.insert(student));

        try {
            guardianDao.link(student.getId(), guardianId, true, true);
        } catch (RuntimeException e) {
            // Compensation: a student without guardian breaks the rule, so we undo the registration.
            studentDao.updateStatus(student.getId(), StudentStatus.WITHDRAWN);
            logger.error(getClass(), "Guardian link failed; student " + student.getId() + " was marked as withdrawn", e);
            throw e;
        }
        logger.info(getClass(), "Student registered: id " + student.getId());
        return student;
    }

    public void updateStudent(Student student) {
        findStudent(student.getId());
        normalizeStudent(student);
        studentDao.findByDocument(student.getDocumentNumber()).ifPresent(other -> {
            if (other.getId() != student.getId()) {
                throw new BusinessRuleException("Another student already has that document.");
            }
        });
        studentDao.update(student);
        logger.info(getClass(), "Student updated: id " + student.getId());
    }

    /** Soft delete: the student and all the history stay in the database, only the status changes. */
    public void withdrawStudent(int studentId) {
        Student student = findStudent(studentId);
        if (student.getStatus() == StudentStatus.WITHDRAWN) {
            throw new BusinessRuleException("The student is already withdrawn.");
        }
        studentDao.updateStatus(studentId, StudentStatus.WITHDRAWN);
        // Active grade enrollments are closed too, so the student stops appearing in lists and reports.
        enrollmentDao.findByStudent(studentId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .map(Enrollment::getId)
                .forEach(id -> enrollmentDao.updateStatus(id, EnrollmentStatus.WITHDRAWN, LocalDate.now()));
        logger.info(getClass(), "Student withdrawn: id " + studentId);
    }

    public Student findStudent(int studentId) {
        return studentDao.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: id " + studentId));
    }

    public Student findByDocument(String documentNumber) {
        return studentDao.findByDocument(Validator.requireDocument(documentNumber))
                .orElseThrow(() -> new NotFoundException("No student with document " + documentNumber));
    }

    public List<Student> searchByName(String text) {
        return studentDao.searchByName(Validator.requireText("Search text", text, 100));
    }

    public List<Student> listStudents() {
        return studentDao.findAll();
    }

    // ------------------------------------------------------------------ guardians

    public List<Guardian> listGuardians(int studentId) {
        findStudent(studentId);
        return guardianDao.findByStudentId(studentId);
    }

    /** Adds another guardian to a student (new guardian or an existing one found by document). */
    public void addGuardian(int studentId, Guardian guardian, boolean primary, boolean emergencyContact) {
        findStudent(studentId);
        normalizeGuardian(guardian);
        int guardianId = resolveGuardianId(guardian);
        boolean alreadyLinked = guardianDao.findByStudentId(studentId).stream()
                .anyMatch(g -> g.getId() == guardianId);
        if (alreadyLinked) {
            throw new BusinessRuleException("That guardian is already linked to the student.");
        }
        guardianDao.link(studentId, guardianId, primary, emergencyContact);
    }

    /** Rule: the last guardian of a student cannot be removed. */
    public void removeGuardian(int studentId, int guardianId) {
        if (guardianDao.countByStudent(studentId) <= 1) {
            throw new BusinessRuleException("A student must keep at least one guardian.");
        }
        if (!guardianDao.unlink(studentId, guardianId)) {
            throw new NotFoundException("That guardian is not linked to the student.");
        }
    }

    public void updateGuardian(Guardian guardian) {
        guardianDao.findById(guardian.getId())
                .orElseThrow(() -> new NotFoundException("Guardian not found: id " + guardian.getId()));
        normalizeGuardian(guardian);
        guardianDao.update(guardian);
    }

    // ------------------------------------------------------------------ private helpers

    /** Returns the id of an existing guardian (by id or document) or inserts the new one. */
    private int resolveGuardianId(Guardian guardian) {
        if (guardian.getId() > 0) {
            return guardian.getId();
        }
        return guardianDao.findByDocument(guardian.getDocumentNumber())
                .map(Guardian::getId)
                .orElseGet(() -> {
                    guardian.setId(guardianDao.insert(guardian));
                    return guardian.getId();
                });
    }

    /** Validates and cleans every field of a student (trim, null for empty optionals). */
    private void normalizeStudent(Student s) {
        Validator.requireNotNull("Document type", s.getDocumentType());
        s.setDocumentNumber(Validator.requireDocument(s.getDocumentType(), s.getDocumentNumber()));
        s.setFirstName(Validator.requirePersonName("First name", s.getFirstName(), 100));
        s.setMiddleName(Validator.optionalPersonName("Middle name", s.getMiddleName(), 100));
        s.setFirstSurname(Validator.requirePersonName("First surname", s.getFirstSurname(), 100));
        s.setSecondSurname(Validator.optionalPersonName("Second surname", s.getSecondSurname(), 100));
        Validator.requirePastDate("Birth date", s.getBirthDate());
        int age = LocalDate.now().getYear() - s.getBirthDate().getYear();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new BusinessRuleException("Student age must be between " + MIN_AGE + " and " + MAX_AGE + " years.");
        }
        Validator.requireNotNull("Gender", s.getGender());
        s.setAddress(Validator.requireText("Address", s.getAddress(), 255));
        s.setPhone(Validator.optionalPhone("Phone", s.getPhone()));
        s.setEmail(Validator.optionalEmail(s.getEmail()));
        s.setBloodType(Validator.optionalBloodType(s.getBloodType()));
        s.setAllergies(Validator.optionalText("Allergies", s.getAllergies(), 500));
        s.setMedicalConditions(Validator.optionalText("Medical conditions", s.getMedicalConditions(), 500));
    }

    private void normalizeGuardian(Guardian g) {
        Validator.requireNotNull("Guardian document type", g.getDocumentType());
        if (g.getDocumentType() == DocumentType.TI) {
            throw new BusinessRuleException("A guardian cannot use a TI (identity card for minors).");
        }
        g.setDocumentNumber(Validator.requireDocument(g.getDocumentType(), g.getDocumentNumber()));
        g.setFirstName(Validator.requirePersonName("Guardian first name", g.getFirstName(), 100));
        g.setMiddleName(Validator.optionalPersonName("Guardian middle name", g.getMiddleName(), 100));
        g.setFirstSurname(Validator.requirePersonName("Guardian first surname", g.getFirstSurname(), 100));
        g.setSecondSurname(Validator.optionalPersonName("Guardian second surname", g.getSecondSurname(), 100));
        g.setRelationship(Validator.requireRelationship(g.getRelationship()));
        g.setPrimaryPhone(Validator.requirePhone("Primary phone", g.getPrimaryPhone()));
        g.setSecondaryPhone(Validator.optionalPhone("Secondary phone", g.getSecondaryPhone()));
        g.setEmail(Validator.optionalEmail(g.getEmail()));
    }
}
