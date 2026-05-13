package com.bdcrm.template;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateStepRequest(
        @Min(1) @Max(7) int stepNumber,
        @Min(0) int dayOffset,
        @NotNull ContactChannel channel,
        @Size(max = 500) String instructions) {
}
