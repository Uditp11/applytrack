package io.github.uditp11.applytrack.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class TransitionMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final TransitionMetrics metrics = new TransitionMetrics(registry);

    @Test
    void eachCommittedMoveIncrementsTheCounterForItsFromAndToStatus() {
        metrics.onStatusChanged(new StatusChanged(7, ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW));
        metrics.onStatusChanged(new StatusChanged(8, ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW));
        metrics.onStatusChanged(new StatusChanged(8, ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER));

        assertThat(registry.get("applytrack.status.transitions").tag("from", "APPLIED").tag("to", "INTERVIEW")
                .counter().count()).isEqualTo(2);
        assertThat(registry.get("applytrack.status.transitions").tag("from", "INTERVIEW").tag("to", "OFFER")
                .counter().count()).isEqualTo(1);
    }
}
