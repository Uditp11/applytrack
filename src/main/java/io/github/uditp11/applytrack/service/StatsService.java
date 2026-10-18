package io.github.uditp11.applytrack.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.dto.StatsResponse;
import io.github.uditp11.applytrack.repository.StatsRepository;
import io.github.uditp11.applytrack.repository.StatsRepository.ResponseCounts;

@Service
public class StatsService {

    private final StatsRepository repository;

    public StatsService(StatsRepository repository) {
        this.repository = repository;
    }

    /** Read-only transaction: both queries see the same snapshot of the data. */
    @Transactional(readOnly = true)
    public StatsResponse stats() {
        Map<ApplicationStatus, Long> found = repository.countByStatus();
        Map<ApplicationStatus, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, found.getOrDefault(status, 0L));
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        ResponseCounts counts = repository.responseCounts();
        Double rate = counts.applied() == 0 ? null
                : BigDecimal.valueOf(counts.responded())
                        .divide(BigDecimal.valueOf(counts.applied()), 4, RoundingMode.HALF_UP)
                        .doubleValue();
        return new StatsResponse(total, byStatus, counts.applied(), counts.responded(), rate);
    }
}
