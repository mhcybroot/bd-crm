package com.bdcrm.reporting;

import java.util.Map;

public record OutcomeSummaryResponse(
        Map<String, Long> outcomes,
        long unknownOutcomeCount) {
}
