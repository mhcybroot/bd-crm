package com.bdcrm.savedview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SavedViewRequest(
        @NotBlank @Size(max = 64) String pageKey,
        @NotBlank @Size(max = 120) String name,
        boolean shared,
        @NotBlank String configJson) {
}
