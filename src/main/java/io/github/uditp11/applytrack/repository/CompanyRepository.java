package io.github.uditp11.applytrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.uditp11.applytrack.domain.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
