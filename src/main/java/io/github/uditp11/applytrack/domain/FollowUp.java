package io.github.uditp11.applytrack.domain;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    private LocalDate dueDate;
    private String note;
    private boolean done;

    protected FollowUp() {
        // for JPA
    }

    public FollowUp(Application application, LocalDate dueDate, String note) {
        this.application = application;
        this.dueDate = dueDate;
        this.note = note;
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public LocalDate getDueDate() { return dueDate; }
    public String getNote() { return note; }
    public boolean isDone() { return done; }

    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setNote(String note) { this.note = note; }
    public void setDone(boolean done) { this.done = done; }
}
