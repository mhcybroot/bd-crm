package com.bdcrm.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FollowupTemplateRequest(
        @NotBlank String name,
        @Size(max = 500) String description,
        boolean isDefault,
        boolean active,
        @Valid @NotEmpty List<TemplateStepRequest> steps) {
}
