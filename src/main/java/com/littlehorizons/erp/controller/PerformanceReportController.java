package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dto.StudentAverage;
import com.littlehorizons.erp.dto.SubjectAverage;
import com.littlehorizons.erp.dto.SubjectGradeRecord;
import com.littlehorizons.erp.dto.SubjectInfo;
import com.littlehorizons.erp.dto.SubjectPerformanceReport;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.Averages;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Statistics about academic performance and attendance (Module 5).
 * Every method is a Stream pipeline over the data read by the DAOs; nothing is stored.
 */
// SOLID: SRP – academic statistics only (the official per-student averages stay in GradeController)
public class PerformanceReportController {

    /** Minimum passing grade in the Colombian scale used by the school. */
    public static final BigDecimal PASSING_SCORE = new BigDecimal("3.0");

    private final AcademicRecordDao recordDao;
    private final AttendanceDao attendanceDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;

    public PerformanceReportController(AcademicRecordDao recordDao, AttendanceDao attendanceDao,
                                       StudentDao studentDao, SubjectDao subjectDao) {
        this.recordDao = recordDao;
        this.attendanceDao = attendanceDao;
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
    }

    /** Best students of a period: general average per student, highest first, limited to "limit" rows. */
    public List<StudentAverage> studentRanking(int academicYear, AcademicPeriod period, int limit) {
        Validator.requireNotNull("Period", period);
        Validator.requirePositive("Limit", limit);
        Map<Integer, String> names = studentDao.findAll().stream()
                .collect(Collectors.toMap(Student::getId, Student::getFullName));

        // groupingBy student -> list of scores -> exact average (BigDecimal) -> sorted descending -> first N
        return recordDao.findByYear(academicYear).stream()
                .filter(r -> r.getPeriod() == period)
                .collect(Collectors.groupingBy(AcademicRecord::getStudentId,
                        Collectors.mapping(AcademicRecord::getScore, Collectors.toList())))
                .entrySet().stream()
                .map(entry -> new StudentAverage(entry.getKey(),
                        names.getOrDefault(entry.getKey(), "Student " + entry.getKey()),
                        Averages.of(entry.getValue()).orElse(BigDecimal.ZERO)))
                .sorted(Comparator.comparing(StudentAverage::average).reversed()
                        .thenComparing(StudentAverage::studentName))
                .limit(limit)
                .toList();
    }

    /** Average of every subject in a year (periods I-IV), ordered from the highest to the lowest average. */
    public Map<String, BigDecimal> subjectAverages(int academicYear) {
        Map<Integer, String> subjectNames = subjectDao.findAll().stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));

        Map<String, BigDecimal> averages = recordDao.findByYear(academicYear).stream()
                .filter(r -> r.getPeriod() != AcademicPeriod.FINAL)
                .collect(Collectors.groupingBy(
                        r -> subjectNames.getOrDefault(r.getSubjectId(), "Subject " + r.getSubjectId()),
                        Collectors.collectingAndThen(
                                Collectors.mapping(AcademicRecord::getScore, Collectors.toList()),
                                scores -> Averages.of(scores).orElse(BigDecimal.ZERO))));

        // sorted by value descending, collected in a LinkedHashMap to keep that order
        return averages.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * General average of every subject over ALL its grades (any student, year and period), ordered from the highest
     * to the lowest, plus the subject(s) with the best average.
     * <p>
     * Flow: AcademicView -> SubjectPerformanceView -> this method -> AcademicRecordDao (JOIN in MySQL).
     * The controller receives the DAO through its constructor (wired once in AppContext), so it never opens a
     * connection itself; it only runs Stream pipelines over the rows the DAO returns.
     * <p>
     * Design decisions:
     * - Scores are averaged as tenths (4.5 -> 45.0): the sum of integer-valued doubles is exact, so
     *   averagingDouble cannot introduce binary rounding errors, and Math.round gives an exact HALF_UP to 1 decimal.
     * - Ties are decided on the rounded average, i.e. on the value the user sees on screen (two subjects that print
     *   4.5 are both "best"). Equal averages are sorted by subject code so the output is deterministic.
     * - No grades means empty maps/lists/Optionals everywhere: no division by zero and no get() on an empty value.
     */
    public SubjectPerformanceReport subjectPerformanceReport() {
        List<SubjectGradeRecord> grades = recordDao.findAllWithSubject();

        // Step 1: group grades by subject and average them (key = subject, value = average in tenths)
        Map<SubjectInfo, Double> averageInTenths = grades.stream()
                .collect(Collectors.groupingBy(SubjectGradeRecord::subject,
                        Collectors.averagingDouble(g -> g.score().movePointRight(1).doubleValue())));

        // Step 2: entries -> DTOs with the average rounded to 1 decimal -> highest first (ties by code)
        List<SubjectAverage> ranking = averageInTenths.entrySet().stream()
                .map(e -> new SubjectAverage(e.getKey(), BigDecimal.valueOf(Math.round(e.getValue()), 1)))
                .sorted(Comparator.comparing(SubjectAverage::average).reversed()
                        .thenComparing(s -> s.subject().code()))
                .toList();

        // Step 3: maximum average (Optional: empty when there are no subjects), then keep EVERY subject that equals it
        Optional<BigDecimal> maxAverage = ranking.stream()
                .map(SubjectAverage::average)
                .max(Comparator.naturalOrder());
        List<SubjectAverage> topSubjects = maxAverage
                .map(max -> ranking.stream().filter(s -> s.average().compareTo(max) == 0).toList())
                .orElseGet(List::of);

        return new SubjectPerformanceReport(ranking, topSubjects);
    }

    /** How many grades pass (3.0 or more) and how many fail in a year. Key true = pass, false = fail. */
    public Map<Boolean, Long> passFailCounts(int academicYear) {
        // partitioningBy always returns both keys, even when one of them has zero elements
        return recordDao.findByYear(academicYear).stream()
                .filter(r -> r.getPeriod() != AcademicPeriod.FINAL)
                .collect(Collectors.partitioningBy(r -> r.getScore().compareTo(PASSING_SCORE) >= 0, Collectors.counting()));
    }

    /**
     * Percentage of attended classes per subject between two dates.
     * averagingDouble is used here because it is a ratio (0 or 1 per record), not money or grades.
     */
    public Map<String, Double> attendanceRateBySubject(LocalDate from, LocalDate to) {
        Validator.requireNotNull("Start date", from);
        Validator.requireNotNull("End date", to);
        if (to.isBefore(from)) {
            throw new ValidationException("The end date cannot be before the start date.");
        }
        Map<Integer, String> subjectNames = subjectDao.findAll().stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));

        return attendanceDao.findBetween(from, to).stream()
                .collect(Collectors.groupingBy(
                        (Attendance a) -> subjectNames.getOrDefault(a.getSubjectId(), "Subject " + a.getSubjectId()),
                        Collectors.averagingDouble(a -> a.getStatus() == AttendanceStatus.ABSENT ? 0.0 : 100.0)));
    }
}
