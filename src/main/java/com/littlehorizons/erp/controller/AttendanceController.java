package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dto.AbsenceAlert;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.Validator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business rules for daily attendance and the weekly absence alert.
 * Alert rule: more than 3 DIFFERENT days with an "Ausente" record inside the same week (Monday to Sunday).
 * Counting different days (not records) avoids inflating the number when a student misses every class of one day.
 */
// SOLID: SRP – attendance only
public class AttendanceController {

    /** More than this number of absent days in a week triggers the alert. */
    public static final int ABSENT_DAYS_THRESHOLD = 3;

    private final AttendanceDao attendanceDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final StudentDao studentDao;

    public AttendanceController(AttendanceDao attendanceDao, SubjectEnrollmentDao subjectEnrollmentDao,
                                StudentDao studentDao) {
        this.attendanceDao = attendanceDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.studentDao = studentDao;
    }

    /** Saves the attendance of a student in a subject on a date; if it already exists it is corrected. */
    public Attendance register(int studentId, int subjectId, LocalDate date, AttendanceStatus status,
                               String observations, Integer teacherId) {
        Validator.requireNotFuture("Date", date);
        Validator.requireNotNull("Attendance status", status);
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ValidationException("There are no classes on Sundays.");
        }
        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found: id " + studentId));

        boolean enrolled = subjectEnrollmentDao.findByStudentAndYear(studentId, date.getYear()).stream()
                .anyMatch(e -> e.getSubjectId() == subjectId && e.getStatus() == EnrollmentStatus.ACTIVE);
        if (!enrolled) {
            throw new BusinessRuleException("The student is not enrolled in that subject.");
        }

        Optional<Attendance> existing = attendanceDao.find(studentId, subjectId, date);
        Attendance attendance = existing.orElseGet(Attendance::new);
        attendance.setStudentId(studentId);
        attendance.setSubjectId(subjectId);
        attendance.setDate(date);
        attendance.setStatus(status);
        attendance.setObservations(Validator.optionalText("Observations", observations, 500));
        attendance.setRegisteredBy(teacherId);

        if (existing.isPresent()) {
            attendanceDao.update(attendance);
        } else {
            attendance.setId(attendanceDao.insert(attendance));
        }
        return attendance;
    }

    public List<Attendance> listByStudent(int studentId) {
        return attendanceDao.findByStudent(studentId);
    }

    /** Percentage of attended classes of a student ("Presente", "Tardanza" and "Excusa Médica" count as attended). */
    public Optional<Double> attendanceRate(int studentId) {
        List<Attendance> records = attendanceDao.findByStudent(studentId);
        if (records.isEmpty()) {
            return Optional.empty();
        }
        long attended = records.stream().filter(a -> a.getStatus() != AttendanceStatus.ABSENT).count();
        return Optional.of(attended * 100.0 / records.size());
    }

    /**
     * Students with more than 3 absent days in one week, between two dates (both included).
     * The result is sorted from the worst case to the lightest one.
     */
    public List<AbsenceAlert> findWorryingAbsences(LocalDate from, LocalDate to) {
        Validator.requireNotNull("Start date", from);
        Validator.requireNotNull("End date", to);
        if (to.isBefore(from)) {
            throw new ValidationException("The end date cannot be before the start date.");
        }
        Map<Integer, String> names = studentDao.findAll().stream()
                .collect(Collectors.toMap(Student::getId, Student::getFullName));

        // Pipeline: only absences -> group by student -> group by Monday of the week -> set of different days
        Map<Integer, Map<LocalDate, Set<LocalDate>>> absentDaysByStudentAndWeek = attendanceDao.findBetween(from, to).stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .collect(Collectors.groupingBy(Attendance::getStudentId,
                        Collectors.groupingBy(a -> weekStart(a.getDate()),
                                Collectors.mapping(Attendance::getDate, Collectors.toSet()))));

        // Pipeline: flatten (student, week, days) -> keep weeks above the threshold -> build alerts -> sort
        return absentDaysByStudentAndWeek.entrySet().stream()
                .flatMap(student -> student.getValue().entrySet().stream()
                        .filter(week -> week.getValue().size() > ABSENT_DAYS_THRESHOLD)
                        .map(week -> new AbsenceAlert(student.getKey(),
                                names.getOrDefault(student.getKey(), "Student " + student.getKey()),
                                week.getKey(), week.getValue().size())))
                .sorted(Comparator.comparingInt(AbsenceAlert::absentDays).reversed()
                        .thenComparing(AbsenceAlert::weekStart))
                .toList();
    }

    /** Monday of the week that contains the date. */
    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
