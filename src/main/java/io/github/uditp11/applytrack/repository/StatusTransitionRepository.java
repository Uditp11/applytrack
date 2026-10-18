package io.github.uditp11.applytrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.uditp11.applytrack.domain.StatusTransition;

public interface StatusTransitionRepository extends JpaRepository<StatusTransition, Long> {

    List<StatusTransition> findByApplicationIdOrderByOccurredAtAscIdAsc(long applicationId);
}
