package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;

import java.util.List;
import java.util.Optional;

/** Data access contract for students. */
public interface StudentDao extends ReadableDao<Student> {

    /** Inserts the student and returns the generated id. */
    int insert(Student student);

    boolean update(Student student);

    /** Used for soft delete: the student is marked as withdrawn instead of being erased. */
    boolean updateStatus(int studentId, StudentStatus status);

    Optional<Student> findByDocument(String documentNumber);

    List<Student> searchByName(String text);

    /** Students with an active enrollment in the given grade and year. */
    List<Student> findByGradeAndYear(int gradeId, int academicYear);
}
