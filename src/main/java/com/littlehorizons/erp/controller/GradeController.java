package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dto.HistoryLine;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.Averages;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business rules for grades (notas) and the averages calculated from them.
 * Averages are never stored: they are calculated from the records every time, so they can never be out of date.
 * The "Final" record is a grade that a teacher registers; averages use only periods I to IV.
 */
// SOLID: SRP – grades and averages only
public class GradeController {

    private final AcademicRecordDao recordDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final TeacherAssignmentDao assignmentDao;
    private final SubjectDao subjectDao;
    private final StudentDao studentDao;

    public GradeController(AcademicRecordDao recordDao, SubjectEnrollmentDao subjectEnrollmentDao,
                           TeacherAssignmentDao assignmentDao, SubjectDao subjectDao, StudentDao studentDao) {
        this.recordDao = recordDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.assignmentDao = assignmentDao;
        this.subjectDao = subjectDao;
        this.studentDao = studentDao;
    }

    // ------------------------------------------------------------------ register grades

    /**
     * Saves a grade. If a grade already exists for (student, subject, year, period) it is corrected instead of duplicated.
     */
    public AcademicRecord registerScore(int studentId, int subjectId, int teacherId, int academicYear,
                                        AcademicPeriod period, BigDecimal score, String observations) {
        Validator.requireNotNull("Period", period);
        if (period == AcademicPeriod.ANNUAL) {
            throw new BusinessRuleException("ANNUAL is only for payments. Use I, II, III, IV or Final.");
        }
        BigDecimal validScore = Validator.requireScore(score);

        studentDao.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found: id " + studentId));
        subjectDao.findById(subjectId).orElseThrow(() -> new NotFoundException("Subject not found: id " + subjectId));
        checkStudentTakesSubject(studentId, subjectId, academicYear, period);
        checkTeacherTeachesSubject(teacherId, subjectId, academicYear);

        Optional<AcademicRecord> existing = recordDao.find(studentId, subjectId, academicYear, period);
        AcademicRecord record = existing.orElseGet(AcademicRecord::new);
        record.setStudentId(studentId);
        record.setSubjectId(subjectId);
        record.setTeacherId(teacherId);
        record.setAcademicYear(academicYear);
        record.setPeriod(period);
        record.setScore(validScore);
        record.setObservations(Validator.optionalText("Observations", observations, 1000));
        record.setRecordDate(LocalDate.now());

        if (existing.isPresent()) {
            recordDao.update(record);
        } else {
            record.setId(recordDao.insert(record));
        }
        return record;
    }

    /** The student must be enrolled in the subject: in that period (I-IV) or in any period (Final). */
    private void checkStudentTakesSubject(int studentId, int subjectId, int academicYear, AcademicPeriod period) {
        List<SubjectEnrollment> enrollments = subjectEnrollmentDao.findByStudentAndYear(studentId, academicYear);
        boolean enrolled = enrollments.stream()
                .filter(e -> e.getSubjectId() == subjectId)
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .anyMatch(e -> period == AcademicPeriod.FINAL || e.getPeriod() == period);
        if (!enrolled) {
            throw new BusinessRuleException("The student is not enrolled in that subject for period " + period + ".");
        }
    }

    private void checkTeacherTeachesSubject(int teacherId, int subjectId, int academicYear) {
        boolean assigned = assignmentDao.findByTeacher(teacherId, academicYear).stream()
                .anyMatch(a -> a.getSubjectId() == subjectId);
        if (!assigned) {
            throw new BusinessRuleException("That teacher is not assigned to the subject in " + academicYear + ".");
        }
    }

    // ------------------------------------------------------------------ averages

    /** Average of one student in one subject over periods I-IV. Empty if there are no grades yet. */
    public Optional<BigDecimal> subjectAverage(int studentId, int subjectId, int academicYear) {
        List<BigDecimal> scores = recordDao.findByStudent(studentId).stream()
                .filter(r -> r.getSubjectId() == subjectId && r.getAcademicYear() == academicYear)
                .filter(r -> isRegularPeriod(r.getPeriod()))   // Final is excluded from the average
                .map(AcademicRecord::getScore)
                .toList();
        return Averages.of(scores);
    }

    /** General average of one student in one period: average of the grades of all his/her subjects. */
    public Optional<BigDecimal> periodAverage(int studentId, int academicYear, AcademicPeriod period) {
        List<BigDecimal> scores = recordDao.findByStudent(studentId).stream()
                .filter(r -> r.getAcademicYear() == academicYear && r.getPeriod() == period)
                .map(AcademicRecord::getScore)
                .toList();
        return Averages.of(scores);
    }

    /** Average of the whole course in a subject, one value per period (I, II, III, IV, Final). */
    public Map<AcademicPeriod, BigDecimal> courseAverageByPeriod(int subjectId, int academicYear) {
        // groupingBy period -> list of scores; then each list is converted to its average
        Map<AcademicPeriod, List<BigDecimal>> scoresByPeriod = recordDao.findBySubjectAndYear(subjectId, academicYear).stream()
                .collect(Collectors.groupingBy(AcademicRecord::getPeriod, () -> new EnumMap<>(AcademicPeriod.class),
                        Collectors.mapping(AcademicRecord::getScore, Collectors.toList())));
        Map<AcademicPeriod, BigDecimal> result = new EnumMap<>(AcademicPeriod.class);
        scoresByPeriod.forEach((period, scores) -> Averages.of(scores).ifPresent(avg -> result.put(period, avg)));
        return result;
    }

    /** Average of the whole course in a subject over periods I-IV (one number). */
    public Optional<BigDecimal> courseAverage(int subjectId, int academicYear) {
        List<BigDecimal> scores = recordDao.findBySubjectAndYear(subjectId, academicYear).stream()
                .filter(r -> isRegularPeriod(r.getPeriod()))
                .map(AcademicRecord::getScore)
                .toList();
        return Averages.of(scores);
    }

    // ------------------------------------------------------------------ history

    /** Complete academic history of a student, ordered by year, subject name and period. */
    public List<HistoryLine> history(int studentId) {
        Map<Integer, String> subjectNames = subjectDao.findAll().stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
        return recordDao.findByStudent(studentId).stream()
                .map(r -> new HistoryLine(r.getAcademicYear(),
                        subjectNames.getOrDefault(r.getSubjectId(), "Subject " + r.getSubjectId()),
                        r.getPeriod(), r.getScore()))
                .sorted(Comparator.comparingInt(HistoryLine::academicYear)
                        .thenComparing(HistoryLine::subjectName)
                        .thenComparing(HistoryLine::period))
                .toList();
    }

    /** Records of a student grouped by subject name (used by the report card screen). */
    public Map<String, List<AcademicRecord>> recordsBySubject(int studentId, int academicYear) {
        Map<Integer, String> subjectNames = subjectDao.findAll().stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
        Function<AcademicRecord, String> subjectName =
                r -> subjectNames.getOrDefault(r.getSubjectId(), "Subject " + r.getSubjectId());
        return recordDao.findByStudent(studentId).stream()
                .filter(r -> r.getAcademicYear() == academicYear)
                .collect(Collectors.groupingBy(subjectName));
    }

    private boolean isRegularPeriod(AcademicPeriod period) {
        return period != AcademicPeriod.FINAL && period != AcademicPeriod.ANNUAL;
    }
}
