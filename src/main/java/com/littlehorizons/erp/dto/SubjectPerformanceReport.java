package com.littlehorizons.erp.dto;

import java.util.List;

/**
 * Result of the "performance by subject" report.
 * ranking: every subject with grades, from the highest to the lowest average.
 * topSubjects: the subject(s) whose average equals the maximum (more than one when there is a tie).
 * Both lists are empty when there are no grades; the view uses isEmpty() to show the "no data" message.
 */
public record SubjectPerformanceReport(List<SubjectAverage> ranking, List<SubjectAverage> topSubjects) {

    public boolean isEmpty() {
        return ranking.isEmpty();
    }
}
