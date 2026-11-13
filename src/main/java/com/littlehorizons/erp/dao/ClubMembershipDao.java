package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClubMembership;

import java.time.LocalDate;
import java.util.List;

/** Data access contract for club memberships. */
public interface ClubMembershipDao extends ReadableDao<ClubMembership> {

    int insert(ClubMembership membership);

    boolean withdraw(int membershipId, LocalDate leaveDate, String reason);

    List<ClubMembership> findByClub(int clubId);

    List<ClubMembership> findByStudent(int studentId);

    /** Active members of a club, used to validate the capacity. */
    int countActiveByClub(int clubId);

    boolean existsActive(int studentId, int clubId);
}
