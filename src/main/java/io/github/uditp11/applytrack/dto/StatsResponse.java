package io.github.uditp11.applytrack.dto;

import java.util.Map;

import io.github.uditp11.applytrack.domain.ApplicationStatus;

/**
 * Counts by current status, and the response rate: of the applications that ever reached APPLIED,
 * the share that ever reached INTERVIEW or OFFER. The rate is null while nothing has been applied.
 */
public record StatsResponse(long total, Map<ApplicationStatus, Long> byStatus, long applied, long responded,
        Double responseRate) {
}
