package com.littlehorizons.erp.model;

import java.time.LocalTime;

/**
 * Plain data holder (POJO) for table(s): horarios_asignacion (new table).
 * It only carries data between layers and has no business logic.
 */
public class ScheduleSlot {

    private int id;
    private int assignmentId;
    private WeekDay day;
    private LocalTime startTime;
    private LocalTime endTime;

    public ScheduleSlot() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public WeekDay getDay() {
        return day;
    }

    public void setDay(WeekDay day) {
        this.day = day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
