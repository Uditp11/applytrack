package io.github.uditp11.applytrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.dto.StatsResponse;
import io.github.uditp11.applytrack.repository.StatsRepository;
import io.github.uditp11.applytrack.repository.StatsRepository.ResponseStats;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsRepository repository;

    @InjectMocks
    private StatsService service;

    @Test
    void withNoApplicationsEverythingIsZeroAndTheRateIsUnknown() {
        when(repository.countByStatus()).thenReturn(Map.of());
        when(repository.responseStats()).thenReturn(new ResponseStats(0, 0, null));

        StatsResponse stats = service.stats();

        assertThat(stats.total()).isZero();
        assertThat(stats.byStatus()).hasSize(6).allSatisfy((status, count) -> assertThat(count).isZero());
        assertThat(stats.responseRate()).isNull();
    }

    @Test
    void missingStatusesAreFilledWithZeroInLifecycleOrder() {
        when(repository.countByStatus())
                .thenReturn(Map.of(ApplicationStatus.APPLIED, 2L, ApplicationStatus.OFFER, 1L));
        when(repository.responseStats()).thenReturn(new ResponseStats(3, 1, 0.3333));

        StatsResponse stats = service.stats();

        assertThat(stats.total()).isEqualTo(3);
        assertThat(stats.byStatus().keySet()).containsExactly(ApplicationStatus.values());
        assertThat(stats.byStatus())
                .containsEntry(ApplicationStatus.DRAFT, 0L)
                .containsEntry(ApplicationStatus.APPLIED, 2L);
    }

    @Test
    void theCountsAndTheRateComeFromTheDatabaseUnchanged() {
        when(repository.countByStatus()).thenReturn(Map.of());
        when(repository.responseStats()).thenReturn(new ResponseStats(3, 2, 0.6667));

        StatsResponse stats = service.stats();

        assertThat(stats.applied()).isEqualTo(3);
        assertThat(stats.responded()).isEqualTo(2);
        assertThat(stats.responseRate()).isEqualTo(0.6667);
    }
}
