package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dto.BatchResult;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Business rules for clubs and club memberships.
 * Rules: a club has a maximum capacity; a student cannot be an active member twice;
 * a student can join several clubs.
 */
// SOLID: SRP – club rules only
public class ClubController {

    private static final int MAX_CAPACITY_LIMIT = 200;

    private final ClubDao clubDao;
    private final ClubMembershipDao membershipDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ClubController(ClubDao clubDao, ClubMembershipDao membershipDao, StudentDao studentDao,
                          TeacherDao teacherDao) {
        this.clubDao = clubDao;
        this.membershipDao = membershipDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ------------------------------------------------------------------ catalog

    public Club createClub(Club club) {
        normalize(club);
        clubDao.findByName(club.getName()).ifPresent(existing -> {
            throw new BusinessRuleException("A club named \"" + club.getName() + "\" already exists.");
        });
        club.setActive(true);
        club.setId(clubDao.insert(club));
        return club;
    }

    public void updateClub(Club club) {
        Club current = findClub(club.getId());
        normalize(club);
        clubDao.findByName(club.getName()).ifPresent(other -> {
            if (other.getId() != club.getId()) {
                throw new BusinessRuleException("A club named \"" + club.getName() + "\" already exists.");
            }
        });
        // The capacity cannot be lowered below the members the club already has
        if (club.getMaxCapacity() < membershipDao.countActiveByClub(current.getId())) {
            throw new BusinessRuleException("The new capacity is lower than the current number of active members.");
        }
        clubDao.update(club);
    }

    public void setActive(int clubId, boolean active) {
        findClub(clubId);
        clubDao.setActive(clubId, active);
    }

    public Club findClub(int clubId) {
        return clubDao.findById(clubId).orElseThrow(() -> new NotFoundException("Club not found: id " + clubId));
    }

    public List<Club> listClubs() {
        return clubDao.findAll();
    }

    /** Free places of a club (capacity minus active members). */
    public int availableSeats(int clubId) {
        return findClub(clubId).getMaxCapacity() - membershipDao.countActiveByClub(clubId);
    }

    // ------------------------------------------------------------------ memberships

    /**
     * Joins one student to one club. synchronized: the capacity check and the insert must not run at the same time
     * for two requests (this console app has one user, so this is enough; see the guide for the multi-user option).
     */
    public synchronized ClubMembership joinClub(int studentId, int clubId) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: id " + studentId));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE students can join clubs.");
        }
        Club club = findClub(clubId);
        if (!club.isActive()) {
            throw new BusinessRuleException("The club \"" + club.getName() + "\" is not active.");
        }
        if (membershipDao.existsActive(studentId, clubId)) {
            throw new BusinessRuleException("The student is already an active member of \"" + club.getName() + "\".");
        }
        if (membershipDao.countActiveByClub(clubId) >= club.getMaxCapacity()) {
            throw new BusinessRuleException("The club \"" + club.getName() + "\" is full (capacity "
                    + club.getMaxCapacity() + ").");
        }

        ClubMembership membership = new ClubMembership();
        membership.setStudentId(studentId);
        membership.setClubId(clubId);
        membership.setJoinDate(LocalDate.now());
        membership.setStatus(EnrollmentStatus.ACTIVE);
        membership.setId(membershipDao.insert(membership));
        return membership;
    }

    /** Multiple enrollment: tries every club and reports which ones accepted the student and why the others did not. */
    public BatchResult joinClubs(int studentId, List<Integer> clubIds) {
        List<Integer> accepted = new ArrayList<>();
        Map<Integer, String> rejected = new LinkedHashMap<>();
        for (int clubId : clubIds) {
            try {
                joinClub(studentId, clubId);
                accepted.add(clubId);
            } catch (ErpException e) {
                rejected.put(clubId, e.getMessage());
            }
        }
        return new BatchResult(accepted, rejected);
    }

    public void leaveClub(int membershipId, String reason) {
        ClubMembership membership = membershipDao.findById(membershipId)
                .orElseThrow(() -> new NotFoundException("Membership not found: id " + membershipId));
        if (membership.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("The membership is not active.");
        }
        membershipDao.withdraw(membershipId, LocalDate.now(), Validator.optionalText("Reason", reason, 500));
    }

    public List<ClubMembership> listMembers(int clubId) {
        return membershipDao.findByClub(clubId);
    }

    public List<ClubMembership> listMembershipsOfStudent(int studentId) {
        return membershipDao.findByStudent(studentId);
    }

    // ------------------------------------------------------------------ private helpers

    private void normalize(Club club) {
        club.setName(Validator.requireText("Club name", club.getName(), 100));
        club.setDescription(Validator.optionalText("Description", club.getDescription(), 1000));
        club.setMeetingSchedule(Validator.optionalText("Meeting schedule", club.getMeetingSchedule(), 100));
        if (club.getMaxCapacity() < 1 || club.getMaxCapacity() > MAX_CAPACITY_LIMIT) {
            throw new BusinessRuleException("Capacity must be between 1 and " + MAX_CAPACITY_LIMIT + ".");
        }
        if (club.getSupervisorId() != null) {
            Teacher supervisor = teacherDao.findById(club.getSupervisorId())
                    .orElseThrow(() -> new NotFoundException("Supervisor teacher not found: id " + club.getSupervisorId()));
            if (supervisor.getStatus() != TeacherStatus.ACTIVE) {
                throw new BusinessRuleException("The supervisor must be an ACTIVE teacher.");
            }
        }
    }
}
