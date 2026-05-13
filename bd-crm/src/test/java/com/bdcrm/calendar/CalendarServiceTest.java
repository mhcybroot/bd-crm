package com.bdcrm.calendar;

import com.bdcrm.followup.FollowupStatus;
import com.bdcrm.followup.LeadFollowup;
import com.bdcrm.followup.LeadFollowupRepository;
import com.bdcrm.lead.Lead;
import com.bdcrm.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private LeadFollowupRepository leadFollowupRepository;

    @InjectMocks
    private CalendarService calendarService;

    private Lead testLead;
    private User testUser;
    private LeadFollowup testFollowup;

    @BeforeEach
    void setUp() {
        testLead = new Lead();
        testLead.setId(1L);
        testLead.setCompanyName("Test Company");
        testLead.setContactName("John Doe");
        testLead.setCreatedAt(OffsetDateTime.now());

        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test User");
        testUser.setUsername("testuser");

        testFollowup = new LeadFollowup();
        testFollowup.setId(1L);
        testFollowup.setLead(testLead);
        testFollowup.setStepNumber(1);
        testFollowup.setDueDate(LocalDate.now().plusDays(1));
        testFollowup.setStatus(FollowupStatus.DUE);
        testFollowup.setChannel(com.bdcrm.template.ContactChannel.CALL);
        testFollowup.setAssignedUser(testUser);
        testFollowup.setInstructions("Call the lead to discuss pricing");
        testFollowup.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void exportFollowupsToIcal_containsCalendarHeader() {
        when(leadFollowupRepository.findAll()).thenReturn(List.of());

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        assertTrue(ical.contains("BEGIN:VCALENDAR"));
        assertTrue(ical.contains("END:VCALENDAR"));
        assertTrue(ical.contains("VERSION:2.0"));
        assertTrue(ical.contains("PRODID:-//BD-CRM//Follow-up Calendar//EN"));
    }

    @Test
    void exportFollowupsToIcal_containsEventForFollowup() {
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        assertTrue(ical.contains("BEGIN:VEVENT"));
        assertTrue(ical.contains("END:VEVENT"));
        assertTrue(ical.contains("UID:followup-1@bd-crm"));
        assertTrue(ical.contains("CATEGORIES:FOLLOWUP"));
    }

    @Test
    void exportFollowupsToIcal_excludesCompletedFollowups() {
        testFollowup.setStatus(FollowupStatus.COMPLETED);
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        assertFalse(ical.contains("BEGIN:VEVENT"));
    }

    @Test
    void exportFollowupsToIcal_excludesCancelledFollowups() {
        testFollowup.setStatus(FollowupStatus.CANCELLED);
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        assertFalse(ical.contains("BEGIN:VEVENT"));
    }

    @Test
    void exportFollowupsToIcal_excludesSkippedFollowups() {
        testFollowup.setStatus(FollowupStatus.SKIPPED);
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        assertFalse(ical.contains("BEGIN:VEVENT"));
    }

    @Test
    void exportFollowupsToIcal_includesActiveFollowups() {
        testFollowup.setStatus(FollowupStatus.OVERDUE);
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        assertTrue(ical.contains("BEGIN:VEVENT"));
    }

    @Test
    void exportFollowupsToIcal_filtersByDateRange() {
        testFollowup.setDueDate(LocalDate.now().plusDays(1));
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        // Date range that doesn't include the followup
        String ical = calendarService.exportFollowupsToIcal(LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(3), testUser, false);

        assertFalse(ical.contains("BEGIN:VEVENT"));
    }

    @Test
    void exportFollowupsToIcal_includesDueFollowups() {
        testFollowup.setDueDate(LocalDate.now());
        testFollowup.setStatus(FollowupStatus.DUE);
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        assertTrue(ical.contains("BEGIN:VEVENT"));
    }

    @Test
    void exportFollowupsToIcal_nonManagerCannotSeeOthersFollowups() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setFullName("Other User");

        testFollowup.setAssignedUser(otherUser);
        testFollowup.setDueDate(LocalDate.now());
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        // Should not include followup assigned to other user
        assertFalse(ical.contains("BEGIN:VEVENT"));
    }

    @Test
    void exportFollowupsToIcal_managerCanSeeAllFollowups() {
        testFollowup.setDueDate(LocalDate.now());
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, true);

        // Manager (managerView=true) should see all followups
        assertTrue(ical.contains("BEGIN:VEVENT"));
    }

    @Test
    void exportFollowupsToIcal_escapesSpecialCharactersInSummary() {
        testFollowup.setLead(testLead);
        testLead.setCompanyName("Test, Company; With\\Special:Characters");
        testFollowup.setDueDate(LocalDate.now());
        when(leadFollowupRepository.findAll()).thenReturn(List.of(testFollowup));

        String ical = calendarService.exportFollowupsToIcal(LocalDate.now(), LocalDate.now().plusMonths(1), testUser, false);

        // Should be escaped properly
        assertTrue(ical.contains("Test\\, Company\\; With\\\\Special\\:Characters"));
    }
}