package com.bdcrm.calendar;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.user.User;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final SecurityUtils securityUtils;

    @GetMapping(value = "/followups.ics", produces = "text/calendar")
    public ResponseEntity<String> exportFollowupsIcs(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to) {

        User currentUser = securityUtils.currentUserEntity();
        boolean managerView = securityUtils.hasAnyRole("ADMIN", "MANAGER");

        LocalDate fromDate = from.isBlank() ? LocalDate.now() : LocalDate.parse(from);
        LocalDate toDate = to.isBlank() ? LocalDate.now().plusMonths(1) : LocalDate.parse(to);

        String ical = calendarService.exportFollowupsToIcal(fromDate, toDate, currentUser, managerView);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"followups.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(ical);
    }
}