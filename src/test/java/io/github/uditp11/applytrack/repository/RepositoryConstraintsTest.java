package io.github.uditp11.applytrack.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.uditp11.applytrack.TestcontainersConfiguration;
import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.Contact;
import io.github.uditp11.applytrack.domain.FollowUp;
import io.github.uditp11.applytrack.domain.Posting;
import io.github.uditp11.applytrack.domain.StatusTransition;

/** Runs against real PostgreSQL with the Flyway schema, so constraints are the database's own. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class RepositoryConstraintsTest {

    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");

    @Autowired CompanyRepository companies;
    @Autowired PostingRepository postings;
    @Autowired ContactRepository contacts;
    @Autowired ApplicationRepository applications;
    @Autowired StatusTransitionRepository transitions;
    @Autowired FollowUpRepository followUps;
    @Autowired TestEntityManager entityManager;

    private Posting newPosting() {
        Company company = companies.save(new Company("Northwind Analytics", "https://northwind.example.com", NOW));
        return postings.save(new Posting(company, "Backend Developer", "Frankfurt", null, NOW));
    }

    /** Writes pending changes and forgets loaded entities, so the next step starts like a separate request. */
    private void startFreshLikeANewRequest() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void savedEntitiesCanBeFoundAgainWithTheirRelations() {
        Posting posting = newPosting();
        Application application = applications.save(new Application(posting, "Referred by a former classmate", NOW));
        transitions.save(new StatusTransition(application, null, ApplicationStatus.DRAFT, NOW));
        followUps.save(new FollowUp(application, LocalDate.of(2026, 10, 25), "Email the recruiter"));
        contacts.save(new Contact(posting.getCompany(), "Mira Okafor", "mira.okafor@example.com", "Recruiter"));
        applications.flush();

        assertThat(applications.findByStatus(ApplicationStatus.DRAFT)).extracting(Application::getId)
                .contains(application.getId());
        assertThat(postings.findByCompanyId(posting.getCompany().getId())).hasSize(1);
        assertThat(contacts.findByCompanyId(posting.getCompany().getId())).hasSize(1);
        assertThat(transitions.findByApplicationIdOrderByOccurredAtAscIdAsc(application.getId()))
                .extracting(StatusTransition::getToStatus).containsExactly(ApplicationStatus.DRAFT);
        assertThat(followUps.findByApplicationId(application.getId())).hasSize(1);
    }

    @Test
    void aPostingCanHaveOnlyOneApplication() {
        Posting posting = newPosting();
        applications.saveAndFlush(new Application(posting, null, NOW));

        assertThatThrownBy(() -> applications.saveAndFlush(new Application(posting, null, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void companyNamesAreUnique() {
        companies.saveAndFlush(new Company("Contoso Freight", null, NOW));

        assertThatThrownBy(() -> companies.saveAndFlush(new Company("Contoso Freight", null, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aCompanyWithPostingsCannotBeDeleted() {
        long companyId = newPosting().getCompany().getId();
        startFreshLikeANewRequest();

        assertThatThrownBy(() -> {
            companies.deleteById(companyId);
            companies.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAnApplicationRemovesItsTransitionsAndFollowUps() {
        Application application = applications.save(new Application(newPosting(), null, NOW));
        transitions.save(new StatusTransition(application, null, ApplicationStatus.DRAFT, NOW));
        followUps.save(new FollowUp(application, LocalDate.of(2026, 10, 25), "Send portfolio link"));
        startFreshLikeANewRequest();

        applications.deleteById(application.getId());
        applications.flush();

        assertThat(transitions.findByApplicationIdOrderByOccurredAtAscIdAsc(application.getId())).isEmpty();
        assertThat(followUps.findByApplicationId(application.getId())).isEmpty();
    }
}
