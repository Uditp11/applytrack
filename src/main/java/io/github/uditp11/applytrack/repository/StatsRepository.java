package io.github.uditp11.applytrack.repository;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import io.github.uditp11.applytrack.domain.ApplicationStatus;

/** Statistics read with plain SQL: aggregates are clearer in SQL than in JPA. */
@Repository
public class StatsRepository {

    /** {@code rate} is null while nothing has been applied. */
    public record ResponseStats(long applied, long responded, Double rate) {
    }

    private final JdbcClient jdbc;

    public StatsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Statuses with no applications are missing from the map. */
    public Map<ApplicationStatus, Long> countByStatus() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        jdbc.sql("select status, count(*) as n from application group by status")
                .query(row -> {
                    counts.put(ApplicationStatus.valueOf(row.getString("status")), row.getLong("n"));
                });
        return counts;
    }

    /**
     * One row per application from its transition history (did it ever reach APPLIED? INTERVIEW or OFFER?),
     * then counted, then divided. History, not current status, so an application rejected after an
     * interview still counts. {@code nullif} turns "nothing applied" into a null rate instead of a division
     * by zero.
     */
    public ResponseStats responseStats() {
        return jdbc.sql("""
                select applied,
                       responded,
                       round(responded::numeric / nullif(applied, 0), 4) as response_rate
                from (
                    select count(*) filter (where reached_applied)                     as applied,
                           count(*) filter (where reached_applied and reached_response) as responded
                    from (
                        select application_id,
                               bool_or(to_status = 'APPLIED')               as reached_applied,
                               bool_or(to_status in ('INTERVIEW', 'OFFER')) as reached_response
                        from status_transition
                        group by application_id
                    ) as history
                ) as counts
                """)
                .query((row, rowNumber) -> {
                    BigDecimal rate = row.getBigDecimal("response_rate");
                    return new ResponseStats(row.getLong("applied"), row.getLong("responded"),
                            rate == null ? null : rate.doubleValue());
                })
                .single();
    }
}
