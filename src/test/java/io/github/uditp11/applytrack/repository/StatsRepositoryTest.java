package io.github.uditp11.applytrack.repository;

import static io.github.uditp11.applytrack.domain.ApplicationStatus.APPLIED;
import static io.github.uditp11.applytrack.domain.ApplicationStatus.DRAFT;
import static io.github.uditp11.applytrack.domain.ApplicationStatus.INTERVIEW;
import static io.github.uditp11.applytrack.domain.ApplicationStatus.OFFER;
import static io.github.uditp11.applytrack.domain.ApplicationStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.uditp11.applytrack.TestcontainersConfiguration;
import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.Posting;
import io.github.uditp11.applytrack.domain.StatusTransition;
import io.github.uditp11.applytrack.repository.StatsRepository.ResponseStats;

/** The statistics SQL against real PostgreSQL. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, StatsRepository.class})
@Testcontainers(disabledWithoutDocker = true)
class StatsRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");

    @Autowired TestEntityManager entityManager;
    @Autowired StatsRepository stats;
    @Autowired JdbcClient jdbc;

    private int postingCounter;

    /** Creates an application that went through the given statuses, recording each move. */
    private void applicationThrough(ApplicationStatus... path) {
        Company company = entityManager.persist(new Company("Stats Test Company " + (++postingCounter), null, NOW));
        Posting posting = entityManager.persist(new Posting(company, "Engineer", null, null, NOW));
        Application application = entityManager.persist(new Application(posting, null, NOW));
        ApplicationStatus from = null;
        for (ApplicationStatus to : path) {
            entityManager.persist(new StatusTransition(application, from, to, NOW));
            application.changeStatus(to, NOW);
            from = to;
        }
        entityManager.flush();
    }

    /*
     * The statistics cover whole tables, and other test classes may have committed rows. Each test starts by
     * deleting everything inside its own transaction; the rollback at the end of the test puts it all back.
     */
    @BeforeEach
    void startFromEmptyTables() {
        jdbc.sql("delete from follow_up").update();
        jdbc.sql("delete from status_transition").update();
        jdbc.sql("delete from application").update();
        jdbc.sql("delete from contact").update();
        jdbc.sql("delete from posting").update();
        jdbc.sql("delete from company").update();
    }

    @Test
    void countsApplicationsByTheirCurrentStatus() {
        applicationThrough(DRAFT);
        applicationThrough(DRAFT, APPLIED);
        applicationThrough(DRAFT, APPLIED, INTERVIEW, REJECTED);

        assertThat(stats.countByStatus())
                .containsExactlyInAnyOrderEntriesOf(Map.of(DRAFT, 1L, APPLIED, 1L, REJECTED, 1L));
    }

    @Test
    void theResponseRateCountsEveryApplicationThatEverReachedInterviewOrOffer() {
        applicationThrough(DRAFT);                                 // never applied: not counted at all
        applicationThrough(DRAFT, APPLIED);                        // applied, no response
        applicationThrough(DRAFT, APPLIED, INTERVIEW, REJECTED);   // responded, even though rejected later
        applicationThrough(DRAFT, APPLIED, INTERVIEW, OFFER);      // responded

        assertThat(stats.responseStats()).isEqualTo(new ResponseStats(3, 2, 0.6667));
    }

    @Test
    void theResponseRateIsNullWhileNothingWasApplied() {
        applicationThrough(DRAFT);

        assertThat(stats.responseStats()).isEqualTo(new ResponseStats(0, 0, null));
    }
}
