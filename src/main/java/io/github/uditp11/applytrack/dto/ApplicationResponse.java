package io.github.uditp11.applytrack.dto;

import java.time.Instant;

import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.ApplicationStatus;

public record ApplicationResponse(Long id, Long postingId, String postingTitle, String companyName,
        ApplicationStatus status, String notes, Instant createdAt, Instant updatedAt) {

    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(application.getId(), application.getPosting().getId(),
                application.getPosting().getTitle(), application.getPosting().getCompany().getName(),
                application.getStatus(), application.getNotes(), application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
