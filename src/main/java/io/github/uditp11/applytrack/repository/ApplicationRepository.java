package io.github.uditp11.applytrack.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.ApplicationStatus;

/**
 * List queries load each application's posting and company in the same SELECT (a join), because the
 * response shows the posting title and company name. Without it, every row costs two extra queries.
 */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Override
    @EntityGraph(attributePaths = {"posting", "posting.company"})
    List<Application> findAll(Sort sort);

    @EntityGraph(attributePaths = {"posting", "posting.company"})
    List<Application> findByStatusOrderById(ApplicationStatus status);
}
