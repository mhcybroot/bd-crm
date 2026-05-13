package com.bdcrm.lead;

import jakarta.validation.constraints.NotBlank;

public record LeadNoteRequest(@NotBlank String body) {
}
