package io.github.uditp11.applytrack.repository;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import io.github.uditp11.applytrack.domain.ApplicationStatus;

/** Statistics read with plain SQL: aggregates are clearer in SQL than in JPA. */
@Repository
public class StatsRepository {

    public record ResponseCounts(long applied, long responded) {
    }

    private final JdbcClient jdbc;

    public StatsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Statuses with no applications are missing from the map. */
    public Map<ApplicationStatus, Long> countByStatus() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        jdbc.sql("select status, count(*) as n from application group by status")
                .query((row, rowNumber) -> counts.put(ApplicationStatus.valueOf(row.getString("status")), row.getLong("n")))
                .list();
        return counts;
    }

    /**
     * One row per application from its transition history (did it ever reach APPLIED? INTERVIEW or OFFER?),
     * then counted. History, not current status, so an application rejected after an interview still counts.
     */
    public ResponseCounts responseCounts() {
        return jdbc.sql("""
                select count(*) filter (where reached_applied)                     as applied,
                       count(*) filter (where reached_applied and reached_response) as responded
                from (
                    select application_id,
                           bool_or(to_status = 'APPLIED')                as reached_applied,
                           bool_or(to_status in ('INTERVIEW', 'OFFER'))  as reached_response
                    from status_transition
                    group by application_id
                ) as history
                """)
                .query((row, rowNumber) -> new ResponseCounts(row.getLong("applied"), row.getLong("responded")))
                .single();
    }
}
