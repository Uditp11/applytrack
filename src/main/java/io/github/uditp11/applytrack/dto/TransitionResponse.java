package io.github.uditp11.applytrack.dto;

import java.time.Instant;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.domain.StatusTransition;

public record TransitionResponse(ApplicationStatus from, ApplicationStatus to, Instant occurredAt) {

    public static TransitionResponse from(StatusTransition transition) {
        return new TransitionResponse(transition.getFromStatus(), transition.getToStatus(),
                transition.getOccurredAt());
    }
}
