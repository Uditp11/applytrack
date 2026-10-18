package io.github.uditp11.applytrack.repository;

import static io.github.uditp11.applytrack.domain.ApplicationStatus.APPLIED;
import static io.github.uditp11.applytrack.domain.ApplicationStatus.DRAFT;
import static io.github.uditp11.applytrack.domain.ApplicationStatus.INTERVIEW;
import static io.github.uditp11.applytrack.domain.ApplicationStatus.OFFER;
import static io.github.uditp11.applytrack.domain.ApplicationStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.uditp11.applytrack.TestcontainersConfiguration;
import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.Posting;
import io.github.uditp11.applytrack.domain.StatusTransition;
import io.github.uditp11.applytrack.repository.StatsRepository.ResponseCounts;

/** The statistics SQL against real PostgreSQL. Each test rolls back, so it only sees its own rows. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, StatsRepository.class})
@Testcontainers(disabledWithoutDocker = true)
class StatsRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");

    @Autowired TestEntityManager entityManager;
    @Autowired StatsRepository stats;

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
     * The statistics cover the whole table, and other test classes may have committed rows before this one
     * runs. So each test measures before and after adding its own applications and checks the difference.
     */

    private long added(Map<ApplicationStatus, Long> before, Map<ApplicationStatus, Long> after,
            ApplicationStatus status) {
        return after.getOrDefault(status, 0L) - before.getOrDefault(status, 0L);
    }

    @Test
    void countsApplicationsByTheirCurrentStatus() {
        Map<ApplicationStatus, Long> before = stats.countByStatus();

        applicationThrough(DRAFT);
        applicationThrough(DRAFT, APPLIED);
        applicationThrough(DRAFT, APPLIED, INTERVIEW, REJECTED);
        Map<ApplicationStatus, Long> after = stats.countByStatus();

        assertThat(added(before, after, DRAFT)).isEqualTo(1);
        assertThat(added(before, after, APPLIED)).isEqualTo(1);
        assertThat(added(before, after, REJECTED)).isEqualTo(1);
        assertThat(added(before, after, INTERVIEW)).isZero();
    }

    @Test
    void respondedCountsEveryApplicationThatEverReachedInterviewOrOffer() {
        ResponseCounts before = stats.responseCounts();

        applicationThrough(DRAFT);                                 // never applied: not counted at all
        applicationThrough(DRAFT, APPLIED);                        // applied, no response
        applicationThrough(DRAFT, APPLIED, INTERVIEW, REJECTED);   // responded, even though rejected later
        applicationThrough(DRAFT, APPLIED, INTERVIEW, OFFER);      // responded
        ResponseCounts after = stats.responseCounts();

        assertThat(after.applied() - before.applied()).isEqualTo(3);
        assertThat(after.responded() - before.responded()).isEqualTo(2);
    }
}
