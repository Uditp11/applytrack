package io.github.uditp11.applytrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.uditp11.applytrack.domain.FollowUp;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    List<FollowUp> findByApplicationIdOrderByDueDateAscIdAsc(long applicationId);
}
