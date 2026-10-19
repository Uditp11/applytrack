package io.github.uditp11.applytrack.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.dto.StatsResponse;
import io.github.uditp11.applytrack.repository.StatsRepository;
import io.github.uditp11.applytrack.repository.StatsRepository.ResponseStats;

@Service
public class StatsService {

    private final StatsRepository repository;

    public StatsService(StatsRepository repository) {
        this.repository = repository;
    }

    /** Repeatable read: both queries see the same snapshot, even if an application moves in between. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StatsResponse stats() {
        Map<ApplicationStatus, Long> found = repository.countByStatus();
        Map<ApplicationStatus, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, found.getOrDefault(status, 0L));
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        ResponseStats response = repository.responseStats();
        return new StatsResponse(total, byStatus, response.applied(), response.responded(), response.rate());
    }
}
