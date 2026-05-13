package com.bdcrm.attachment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentLifecycleRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull DocumentStatus status) {
}
