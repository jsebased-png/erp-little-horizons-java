package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.TeacherAssignment;

import java.util.List;

/** Data access contract for teacher-subject assignments and their weekly schedule slots. */
public interface TeacherAssignmentDao extends ReadableDao<TeacherAssignment> {

    int insert(TeacherAssignment assignment);

    boolean delete(int assignmentId);

    List<TeacherAssignment> findByTeacher(int teacherId, int academicYear);

    List<TeacherAssignment> findBySubject(int subjectId, int academicYear);

    int insertSlot(ScheduleSlot slot);

    boolean deleteSlot(int slotId);

    List<ScheduleSlot> findSlotsByAssignment(int assignmentId);

    /** All slots of a teacher in a year, used to detect schedule conflicts. */
    List<ScheduleSlot> findSlotsByTeacher(int teacherId, int academicYear);
}
