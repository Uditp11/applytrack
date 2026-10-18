package io.github.uditp11.applytrack.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Counts status moves for Prometheus as {@code applytrack_status_transitions_total{from, to}}.
 * It listens after the transaction commits, so a move that is rolled back is never counted.
 */
@Component
public class TransitionMetrics {

    private final MeterRegistry registry;

    public TransitionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @TransactionalEventListener
    public void onStatusChanged(StatusChanged event) {
        Counter.builder("applytrack.status.transitions")
                .description("Application status changes")
                .tag("from", event.from().name())
                .tag("to", event.to().name())
                .register(registry)
                .increment();
    }
}
