package io.github.uditp11.applytrack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationCreateRequest(@NotNull Long postingId, @Size(max = 5000) String notes) {
}
