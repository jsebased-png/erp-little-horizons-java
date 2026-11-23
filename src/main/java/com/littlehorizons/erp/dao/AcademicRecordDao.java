package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.dto.SubjectGradeRecord;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicRecord;

import java.util.List;
import java.util.Optional;

/** Data access contract for grades (table registros_academicos). Averages are calculated in the controller. */
public interface AcademicRecordDao extends ReadableDao<AcademicRecord> {

    int insert(AcademicRecord record);

    boolean update(AcademicRecord record);

    Optional<AcademicRecord> find(int studentId, int subjectId, int academicYear, AcademicPeriod period);

    List<AcademicRecord> findByStudent(int studentId);

    List<AcademicRecord> findBySubjectAndYear(int subjectId, int academicYear);

    List<AcademicRecord> findByYear(int academicYear);

    /** Every grade of the school (any student, year and period) joined with its subject. Empty list when there are none. */
    List<SubjectGradeRecord> findAllWithSubject();
}
