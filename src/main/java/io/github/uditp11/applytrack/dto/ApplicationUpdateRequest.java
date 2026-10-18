package io.github.uditp11.applytrack.dto;

import jakarta.validation.constraints.Size;

/** Only the notes can be edited directly. The status changes through transitions. */
public record ApplicationUpdateRequest(@Size(max = 5000) String notes) {
}
