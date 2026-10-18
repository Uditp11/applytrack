package io.github.uditp11.applytrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.Posting;
import io.github.uditp11.applytrack.domain.StatusTransition;
import io.github.uditp11.applytrack.dto.ApplicationCreateRequest;
import io.github.uditp11.applytrack.dto.ApplicationResponse;
import io.github.uditp11.applytrack.repository.ApplicationRepository;
import io.github.uditp11.applytrack.repository.PostingRepository;
import io.github.uditp11.applytrack.repository.StatusTransitionRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final Instant CREATED = Instant.parse("2026-10-01T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");

    @Mock
    private ApplicationRepository applications;

    @Mock
    private PostingRepository postings;

    @Mock
    private StatusTransitionRepository transitions;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applications, postings, transitions, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Posting posting() {
        return new Posting(new Company("Northwind Analytics", null, CREATED), "Backend Developer", "Frankfurt",
                null, CREATED);
    }

    private static Application applicationIn(ApplicationStatus status) {
        Application application = new Application(posting(), null, CREATED);
        application.changeStatus(status, CREATED);
        return application;
    }

    @Test
    void aLegalMoveChangesTheStatusAndRecordsOneTransition() {
        Application application = applicationIn(ApplicationStatus.APPLIED);
        when(applications.findById(7L)).thenReturn(Optional.of(application));

        ApplicationResponse moved = service.moveTo(7, ApplicationStatus.INTERVIEW);

        assertThat(moved.status()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(moved.updatedAt()).isEqualTo(NOW);
        ArgumentCaptor<StatusTransition> saved = ArgumentCaptor.forClass(StatusTransition.class);
        verify(transitions).save(saved.capture());
        assertThat(saved.getValue().getFromStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(saved.getValue().getToStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void anIllegalMoveThrowsAndChangesNothing() {
        Application application = applicationIn(ApplicationStatus.OFFER);
        when(applications.findById(7L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.moveTo(7, ApplicationStatus.APPLIED))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessage("Cannot move application 7 from OFFER to APPLIED");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.OFFER);
        verifyNoInteractions(transitions);
    }

    @Test
    void movingAnUnknownApplicationThrowsNotFound() {
        when(applications.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moveTo(7, ApplicationStatus.APPLIED))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application 7 not found");
    }

    @Test
    void createStartsInDraftAndRecordsTheFirstTransition() {
        when(postings.findById(3L)).thenReturn(Optional.of(posting()));
        when(applications.save(any(Application.class))).thenAnswer(call -> call.getArgument(0));

        ApplicationResponse created = service.create(new ApplicationCreateRequest(3L, "Found on the company site"));

        assertThat(created.status()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(created.createdAt()).isEqualTo(NOW);
        ArgumentCaptor<StatusTransition> saved = ArgumentCaptor.forClass(StatusTransition.class);
        verify(transitions).save(saved.capture());
        assertThat(saved.getValue().getFromStatus()).isNull();
        assertThat(saved.getValue().getToStatus()).isEqualTo(ApplicationStatus.DRAFT);
    }
}
