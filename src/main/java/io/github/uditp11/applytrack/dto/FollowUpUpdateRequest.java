package io.github.uditp11.applytrack.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A follow-up stays with the application it was created for, so only these fields can change. */
public record FollowUpUpdateRequest(
        @NotNull LocalDate dueDate,
        @NotBlank @Size(max = 500) String note,
        boolean done) {
}
