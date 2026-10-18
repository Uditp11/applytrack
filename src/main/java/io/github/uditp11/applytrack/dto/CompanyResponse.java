package io.github.uditp11.applytrack.dto;

import java.time.Instant;

import io.github.uditp11.applytrack.domain.Company;

public record CompanyResponse(Long id, String name, String website, Instant createdAt) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getWebsite(), company.getCreatedAt());
    }
}
