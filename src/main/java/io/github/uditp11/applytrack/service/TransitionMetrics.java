package io.github.uditp11.applytrack.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Counts status moves for Prometheus as {@code applytrack_status_transitions_total{from, to}}.
 * It listens after the transaction commits, so a move that is rolled back is never counted.
 */
@Component
public class TransitionMetrics {

    private static final String NAME = "applytrack.status.transitions";

    private final MeterRegistry registry;

    /**
     * Creates every allowed pair at 0 up front. Prometheus computes increase() from the difference between
     * samples, so a series that first appears already at 1 would hide that first move.
     */
    public TransitionMetrics(MeterRegistry registry) {
        this.registry = registry;
        for (ApplicationStatus from : ApplicationStatus.values()) {
            for (ApplicationStatus to : ApplicationStatus.values()) {
                if (from.canMoveTo(to)) {
                    counter(from, to);
                }
            }
        }
    }

    @TransactionalEventListener
    public void onStatusChanged(StatusChanged event) {
        counter(event.from(), event.to()).increment();
    }

    private Counter counter(ApplicationStatus from, ApplicationStatus to) {
        return Counter.builder(NAME)
                .description("Application status changes")
                .tag("from", from.name())
                .tag("to", to.name())
                .register(registry);
    }
}
