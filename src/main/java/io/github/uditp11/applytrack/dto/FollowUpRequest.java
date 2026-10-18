package io.github.uditp11.applytrack.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FollowUpRequest(
        @NotNull Long applicationId,
        @NotNull LocalDate dueDate,
        @NotBlank @Size(max = 500) String note,
        boolean done) {
}
