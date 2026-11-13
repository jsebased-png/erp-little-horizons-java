package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.Validator;

import java.util.List;

/**
 * Business rules for the catalog of subjects and the list of grades (Preescolar to Once).
 * It exists so the other controllers and the views can read subjects without touching the DAOs.
 */
// SOLID: SRP – catalog data only
public class SubjectController {

    private static final int MAX_WEEKLY_HOURS = 40;

    private final SubjectDao subjectDao;
    private final GradeDao gradeDao;

    public SubjectController(SubjectDao subjectDao, GradeDao gradeDao) {
        this.subjectDao = subjectDao;
        this.gradeDao = gradeDao;
    }

    public List<Grade> listGrades() {
        return gradeDao.findAll();
    }

    public Grade findGrade(int gradeId) {
        return gradeDao.findById(gradeId).orElseThrow(() -> new NotFoundException("Grade not found: id " + gradeId));
    }

    public List<Subject> listSubjects() {
        return subjectDao.findAll();
    }

    public List<Subject> listSubjectsByGrade(int gradeId) {
        findGrade(gradeId);
        return subjectDao.findByGrade(gradeId);
    }

    public Subject findSubject(int subjectId) {
        return subjectDao.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Subject not found: id " + subjectId));
    }

    public Subject createSubject(Subject subject) {
        normalize(subject);
        subjectDao.findByCode(subject.getCode()).ifPresent(existing -> {
            throw new BusinessRuleException("Subject code " + subject.getCode() + " already exists.");
        });
        subject.setActive(true);
        subject.setId(subjectDao.insert(subject));
        return subject;
    }

    public void updateSubject(Subject subject) {
        findSubject(subject.getId());
        normalize(subject);
        subjectDao.findByCode(subject.getCode()).ifPresent(other -> {
            if (other.getId() != subject.getId()) {
                throw new BusinessRuleException("Subject code " + subject.getCode() + " already exists.");
            }
        });
        subjectDao.update(subject);
    }

    public void setActive(int subjectId, boolean active) {
        findSubject(subjectId);
        subjectDao.setActive(subjectId, active);
    }

    private void normalize(Subject s) {
        s.setCode(Validator.requireText("Subject code", s.getCode(), 20).toUpperCase());
        s.setName(Validator.requireText("Subject name", s.getName(), 100));
        s.setDescription(Validator.optionalText("Description", s.getDescription(), 1000));
        findGrade(s.getGradeId());
        if (s.getWeeklyHours() < 1 || s.getWeeklyHours() > MAX_WEEKLY_HOURS) {
            throw new BusinessRuleException("Weekly hours must be between 1 and " + MAX_WEEKLY_HOURS + ".");
        }
    }
}
