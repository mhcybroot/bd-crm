package com.bdcrm.reporting;

import java.time.LocalDate;

public record TrendPointResponse(
        LocalDate date,
        long leadsCreated,
        long followupsCompleted,
        long wonCount,
        long lostCount) {
}
