package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;

import java.util.Optional;

/** Data access contract for teachers. */
public interface TeacherDao extends ReadableDao<Teacher> {

    int insert(Teacher teacher);

    boolean update(Teacher teacher);

    boolean updateStatus(int teacherId, TeacherStatus status);

    Optional<Teacher> findByDocument(String documentNumber);
}
