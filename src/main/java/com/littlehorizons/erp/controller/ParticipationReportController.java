package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dto.NamedCount;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventPosition;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.Validator;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Statistics about participation in clubs and events (Module 5).
 * Every method is a Stream pipeline; the results are small records the view can print directly.
 */
// SOLID: SRP – extracurricular statistics only
public class ParticipationReportController {

    private static final Set<EventPosition> PODIUM =
            Set.of(EventPosition.FIRST, EventPosition.SECOND, EventPosition.THIRD);

    private final ClubDao clubDao;
    private final ClubMembershipDao membershipDao;
    private final EventDao eventDao;
    private final EventParticipantDao participantDao;
    private final StudentDao studentDao;

    public ParticipationReportController(ClubDao clubDao, ClubMembershipDao membershipDao, EventDao eventDao,
                                         EventParticipantDao participantDao, StudentDao studentDao) {
        this.clubDao = clubDao;
        this.membershipDao = membershipDao;
        this.eventDao = eventDao;
        this.participantDao = participantDao;
        this.studentDao = studentDao;
    }

    /** Active members per club (clubs without members appear with 0), the biggest club first. */
    public List<NamedCount> membersPerClub() {
        Map<Integer, Long> activeMembers = membershipDao.findAll().stream()
                .filter(m -> m.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.groupingBy(ClubMembership::getClubId, Collectors.counting()));
        return clubDao.findAll().stream()
                .map(club -> new NamedCount(club.getName(), activeMembers.getOrDefault(club.getId(), 0L)))
                .sorted(Comparator.comparingLong(NamedCount::count).reversed().thenComparing(NamedCount::name))
                .toList();
    }

    /** Percentage of the capacity used by each active club, e.g. "Chess Club" -> 75 (%). */
    public Map<String, Long> occupancyPercentPerClub() {
        Map<Integer, Long> activeMembers = membershipDao.findAll().stream()
                .filter(m -> m.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.groupingBy(ClubMembership::getClubId, Collectors.counting()));
        return clubDao.findAll().stream()
                .filter(Club::isActive)
                .collect(Collectors.toMap(Club::getName,
                        club -> activeMembers.getOrDefault(club.getId(), 0L) * 100 / club.getMaxCapacity(),
                        (a, b) -> a));
    }

    /** Students who belong to the most clubs at the same time. */
    public List<NamedCount> studentsWithMostClubs(int limit) {
        Validator.requirePositive("Limit", limit);
        Map<Integer, String> names = studentNames();
        return membershipDao.findAll().stream()
                .filter(m -> m.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.groupingBy(ClubMembership::getStudentId, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new NamedCount(names.getOrDefault(e.getKey(), "Student " + e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(NamedCount::count).reversed().thenComparing(NamedCount::name))
                .limit(limit)
                .toList();
    }

    /** Participants per event, the most attended first. */
    public List<NamedCount> participantsPerEvent() {
        Map<Integer, Long> participants = participantDao.findAll().stream()
                .collect(Collectors.groupingBy(EventParticipant::getEventId, Collectors.counting()));
        return eventDao.findAll().stream()
                .map(event -> new NamedCount(event.getName(), participants.getOrDefault(event.getId(), 0L)))
                .sorted(Comparator.comparingLong(NamedCount::count).reversed().thenComparing(NamedCount::name))
                .toList();
    }

    /** Total participations grouped by event type (Académico, Cultural, Deportivo...). */
    public Map<EventType, Long> participationByEventType() {
        Map<Integer, EventType> typeByEvent = eventDao.findAll().stream()
                .collect(Collectors.toMap(SchoolEvent::getId, SchoolEvent::getType));
        return participantDao.findAll().stream()
                .filter(p -> typeByEvent.containsKey(p.getEventId()))
                .collect(Collectors.groupingBy(p -> typeByEvent.get(p.getEventId()),
                        () -> new EnumMap<>(EventType.class), Collectors.counting()));
    }

    /** Students with the most podium finishes (1er, 2do or 3er Lugar). */
    public List<NamedCount> podiumRanking(int limit) {
        Validator.requirePositive("Limit", limit);
        Map<Integer, String> names = studentNames();
        return participantDao.findAll().stream()
                .filter(p -> p.getPosition() != null && PODIUM.contains(p.getPosition()))
                .collect(Collectors.groupingBy(EventParticipant::getStudentId, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new NamedCount(names.getOrDefault(e.getKey(), "Student " + e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(NamedCount::count).reversed().thenComparing(NamedCount::name))
                .limit(limit)
                .toList();
    }

    private Map<Integer, String> studentNames() {
        return studentDao.findAll().stream().collect(Collectors.toMap(Student::getId, Student::getFullName));
    }
}
