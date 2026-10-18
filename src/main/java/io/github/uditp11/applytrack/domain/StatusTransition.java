package io.github.uditp11.applytrack.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** One recorded move of an application. The first one has no from status (creation in DRAFT). */
@Entity
public class StatusTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus toStatus;

    private Instant occurredAt;

    protected StatusTransition() {
        // for JPA
    }

    public StatusTransition(Application application, ApplicationStatus fromStatus, ApplicationStatus toStatus,
            Instant occurredAt) {
        this.application = application;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public ApplicationStatus getFromStatus() { return fromStatus; }
    public ApplicationStatus getToStatus() { return toStatus; }
    public Instant getOccurredAt() { return occurredAt; }
}
