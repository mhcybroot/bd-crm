package com.bdcrm.dashboard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardFilterRequest {

    private String dateFrom;
    private String dateTo;
    private Long repUserId;
    private String leadStatus;
    private String followupOutcome;

    public java.time.LocalDate effectiveDateFrom() {
        return dateFrom != null && !dateFrom.isBlank()
                ? java.time.LocalDate.parse(dateFrom)
                : java.time.LocalDate.now().minusDays(30);
    }

    public java.time.LocalDate effectiveDateTo() {
        return dateTo != null && !dateTo.isBlank()
                ? java.time.LocalDate.parse(dateTo)
                : java.time.LocalDate.now();
    }
}