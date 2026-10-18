package io.github.uditp11.applytrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStatus(ApplicationStatus status);
}
