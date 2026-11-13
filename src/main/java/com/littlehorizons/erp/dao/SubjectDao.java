package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.util.List;
import java.util.Optional;

/** Data access contract for subjects. */
public interface SubjectDao extends ReadableDao<Subject> {

    int insert(Subject subject);

    boolean update(Subject subject);

    boolean setActive(int subjectId, boolean active);

    Optional<Subject> findByCode(String code);

    List<Subject> findByGrade(int gradeId);
}
