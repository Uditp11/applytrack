package io.github.uditp11.applytrack.dto;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@NotNull ApplicationStatus status) {
}
