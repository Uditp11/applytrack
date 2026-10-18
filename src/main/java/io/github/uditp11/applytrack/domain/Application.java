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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

/** One attempt to get one posting. New applications start in DRAFT. */
@Entity
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id")
    private Posting posting;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    /** Optimistic locking: if two requests change the same application at once, the second one fails. */
    @Version
    private long version;

    protected Application() {
        // for JPA
    }

    public Application(Posting posting, String notes, Instant createdAt) {
        this.posting = posting;
        this.notes = notes;
        this.status = ApplicationStatus.DRAFT;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /** Sets the new status without checking the rules. ApplicationService checks them first. */
    public void changeStatus(ApplicationStatus newStatus, Instant at) {
        this.status = newStatus;
        this.updatedAt = at;
    }

    public void changeNotes(String notes, Instant at) {
        this.notes = notes;
        this.updatedAt = at;
    }

    public Long getId() { return id; }
    public Posting getPosting() { return posting; }
    public ApplicationStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
