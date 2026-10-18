package io.github.uditp11.applytrack.service;

import io.github.uditp11.applytrack.domain.ApplicationStatus;

/** Thrown when a status move is not allowed by the state machine. Answered with HTTP 409. */
public class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(long applicationId, ApplicationStatus from, ApplicationStatus to) {
        super("Cannot move application " + applicationId + " from " + from + " to " + to);
    }
}
