package io.github.uditp11.applytrack.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    private String title;
    private String location;
    private String url;
    private Instant createdAt;

    protected Posting() {
        // for JPA
    }

    public Posting(Company company, String title, String location, String url, Instant createdAt) {
        this.company = company;
        this.title = title;
        this.location = location;
        this.url = url;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getUrl() { return url; }
    public Instant getCreatedAt() { return createdAt; }

    public void setCompany(Company company) { this.company = company; }
    public void setTitle(String title) { this.title = title; }
    public void setLocation(String location) { this.location = location; }
    public void setUrl(String url) { this.url = url; }
}
