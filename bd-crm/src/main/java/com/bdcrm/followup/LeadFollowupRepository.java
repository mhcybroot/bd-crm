package com.bdcrm.followup;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadFollowupRepository extends JpaRepository<LeadFollowup, Long> {

    @EntityGraph(attributePaths = {"lead", "lead.assignedUser", "assignedUser"})
    List<LeadFollowup> findByLeadIdOrderByStepNumberAsc(Long leadId);

    @EntityGraph(attributePaths = {"lead", "lead.assignedUser", "assignedUser"})
    Optional<LeadFollowup> findById(Long id);

    @EntityGraph(attributePaths = {"lead", "lead.assignedUser", "assignedUser"})
    List<LeadFollowup> findByStatusInAndDueDateLessThanEqualOrderByDueDateAsc(List<FollowupStatus> statuses, LocalDate dueDate);

    @EntityGraph(attributePaths = {"lead", "lead.assignedUser", "assignedUser"})
    List<LeadFollowup> findByStatusInOrderByDueDateAsc(List<FollowupStatus> statuses);

    boolean existsByLeadIdAndStepNumber(Long leadId, int stepNumber);
}
