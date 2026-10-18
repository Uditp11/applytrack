package io.github.uditp11.applytrack.service;

import io.github.uditp11.applytrack.domain.ApplicationStatus;

/** Published when an application moves to a new status. */
public record StatusChanged(long applicationId, ApplicationStatus from, ApplicationStatus to) {
}
