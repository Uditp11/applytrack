package io.github.uditp11.applytrack.domain;

import java.util.Set;

/**
 * Where an application stands. The allowed moves form a small state machine:
 * DRAFT to APPLIED, APPLIED to INTERVIEW or REJECTED, INTERVIEW to OFFER or REJECTED,
 * and any open status to WITHDRAWN. OFFER, REJECTED and WITHDRAWN are final.
 */
public enum ApplicationStatus {
    DRAFT, APPLIED, INTERVIEW, OFFER, REJECTED, WITHDRAWN;

    public boolean canMoveTo(ApplicationStatus target) {
        return nextStatuses().contains(target);
    }

    public boolean isFinal() {
        return nextStatuses().isEmpty();
    }

    private Set<ApplicationStatus> nextStatuses() {
        return switch (this) {
            case DRAFT -> Set.of(APPLIED, WITHDRAWN);
            case APPLIED -> Set.of(INTERVIEW, REJECTED, WITHDRAWN);
            case INTERVIEW -> Set.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER, REJECTED, WITHDRAWN -> Set.of();
        };
    }
}
