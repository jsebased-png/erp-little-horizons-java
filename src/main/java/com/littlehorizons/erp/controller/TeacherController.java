package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.model.WeekDay;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Business rules for teachers, their subject assignments and their weekly schedule.
 * Schedule rule: a teacher cannot have two time slots that overlap on the same day.
 */
// SOLID: SRP – teacher rules only
public class TeacherController {

    private final TeacherDao teacherDao;
    private final TeacherAssignmentDao assignmentDao;
    private final SubjectDao subjectDao;

    public TeacherController(TeacherDao teacherDao, TeacherAssignmentDao assignmentDao, SubjectDao subjectDao) {
        this.teacherDao = teacherDao;
        this.assignmentDao = assignmentDao;
        this.subjectDao = subjectDao;
    }

    // ------------------------------------------------------------------ directory

    public Teacher registerTeacher(Teacher teacher) {
        normalize(teacher);
        teacherDao.findByDocument(teacher.getDocumentNumber()).ifPresent(existing -> {
            throw new BusinessRuleException("A teacher with that document already exists.");
        });
        teacher.setStatus(TeacherStatus.ACTIVE);
        if (teacher.getHireDate() == null) {
            teacher.setHireDate(LocalDate.now());
        }
        teacher.setId(teacherDao.insert(teacher));
        return teacher;
    }

    public void updateTeacher(Teacher teacher) {
        findTeacher(teacher.getId());
        normalize(teacher);
        teacherDao.findByDocument(teacher.getDocumentNumber()).ifPresent(other -> {
            if (other.getId() != teacher.getId()) {
                throw new BusinessRuleException("Another teacher already has that document.");
            }
        });
        teacherDao.update(teacher);
    }

    public void changeStatus(int teacherId, TeacherStatus status) {
        findTeacher(teacherId);
        teacherDao.updateStatus(teacherId, Validator.requireNotNull("Status", status));
    }

    public Teacher findTeacher(int teacherId) {
        return teacherDao.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("Teacher not found: id " + teacherId));
    }

    public List<Teacher> listTeachers() {
        return teacherDao.findAll();
    }

    // ------------------------------------------------------------------ assignments and schedule

    public TeacherAssignment assignSubject(int teacherId, int subjectId, int academicYear, String section) {
        Teacher teacher = findTeacher(teacherId);
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE teachers can receive subjects.");
        }
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Subject not found: id " + subjectId));
        if (!subject.isActive()) {
            throw new BusinessRuleException("The subject is not active.");
        }
        String cleanSection = Validator.requireText("Section", section, 10).toUpperCase();
        boolean duplicated = assignmentDao.findByTeacher(teacherId, academicYear).stream()
                .anyMatch(a -> a.getSubjectId() == subjectId && cleanSection.equalsIgnoreCase(a.getSection()));
        if (duplicated) {
            throw new BusinessRuleException("The teacher already has that subject and section in " + academicYear + ".");
        }

        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setTeacherId(teacherId);
        assignment.setSubjectId(subjectId);
        assignment.setAcademicYear(academicYear);
        assignment.setSection(cleanSection);
        assignment.setId(assignmentDao.insert(assignment));
        return assignment;
    }

    public void removeAssignment(int assignmentId) {
        if (!assignmentDao.delete(assignmentId)) {
            throw new NotFoundException("Assignment not found: id " + assignmentId);
        }
    }

    public List<TeacherAssignment> listAssignments(int teacherId, int academicYear) {
        return assignmentDao.findByTeacher(teacherId, academicYear);
    }

    /** Adds a weekly time slot to an assignment, rejecting it if it overlaps another slot of the same teacher. */
    public ScheduleSlot addScheduleSlot(int assignmentId, WeekDay day, LocalTime start, LocalTime end) {
        Validator.requireNotNull("Day", day);
        Validator.requireNotNull("Start time", start);
        Validator.requireNotNull("End time", end);
        if (!end.isAfter(start)) {
            throw new ValidationException("The end time must be after the start time.");
        }
        TeacherAssignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found: id " + assignmentId));

        ScheduleSlot conflict = assignmentDao.findSlotsByTeacher(assignment.getTeacherId(), assignment.getAcademicYear())
                .stream()
                .filter(slot -> slot.getDay() == day)
                .filter(slot -> overlaps(slot.getStartTime(), slot.getEndTime(), start, end))
                .findFirst()
                .orElse(null);
        if (conflict != null) {
            throw new BusinessRuleException("Schedule conflict: the teacher already has a class on " + day + " from "
                    + conflict.getStartTime() + " to " + conflict.getEndTime() + ".");
        }

        ScheduleSlot slot = new ScheduleSlot();
        slot.setAssignmentId(assignmentId);
        slot.setDay(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setId(assignmentDao.insertSlot(slot));
        return slot;
    }

    public void removeScheduleSlot(int slotId) {
        if (!assignmentDao.deleteSlot(slotId)) {
            throw new NotFoundException("Schedule slot not found: id " + slotId);
        }
    }

    public List<ScheduleSlot> listSlots(int assignmentId) {
        return assignmentDao.findSlotsByAssignment(assignmentId);
    }

    /** Two ranges overlap when each one starts before the other one ends (touching ends are allowed). */
    private boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private void normalize(Teacher t) {
        Validator.requireNotNull("Document type", t.getDocumentType());
        if (t.getDocumentType() == DocumentType.TI) {
            throw new BusinessRuleException("A teacher cannot use a TI (identity card for minors).");
        }
        t.setDocumentNumber(Validator.requireDocument(t.getDocumentType(), t.getDocumentNumber()));
        t.setFirstName(Validator.requirePersonName("First name", t.getFirstName(), 100));
        t.setMiddleName(Validator.optionalPersonName("Middle name", t.getMiddleName(), 100));
        t.setFirstSurname(Validator.requirePersonName("First surname", t.getFirstSurname(), 100));
        t.setSecondSurname(Validator.optionalPersonName("Second surname", t.getSecondSurname(), 100));
        t.setEmail(Validator.requireEmail(t.getEmail()));
        t.setPhone(Validator.optionalPhone("Phone", t.getPhone()));
        t.setSpecialization(Validator.optionalText("Specialization", t.getSpecialization(), 100));
    }
}
