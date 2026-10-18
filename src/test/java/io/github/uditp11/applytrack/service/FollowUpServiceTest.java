package io.github.uditp11.applytrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.FollowUp;
import io.github.uditp11.applytrack.domain.Posting;
import io.github.uditp11.applytrack.dto.FollowUpRequest;
import io.github.uditp11.applytrack.dto.FollowUpResponse;
import io.github.uditp11.applytrack.dto.FollowUpUpdateRequest;
import io.github.uditp11.applytrack.repository.ApplicationRepository;
import io.github.uditp11.applytrack.repository.FollowUpRepository;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    private static final Instant CREATED = Instant.parse("2026-10-01T08:00:00Z");
    private static final LocalDate DUE = LocalDate.of(2026, 10, 25);

    @Mock
    private FollowUpRepository followUps;

    @Mock
    private ApplicationRepository applications;

    @InjectMocks
    private FollowUpService service;

    private static Application application() {
        Posting posting = new Posting(new Company("Contoso Freight", null, CREATED), "Data Engineer", "Remote",
                null, CREATED);
        return new Application(posting, null, CREATED);
    }

    @Test
    void createForAnUnknownApplicationThrowsNotFoundAndSavesNothing() {
        when(applications.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new FollowUpRequest(999L, DUE, "Email the recruiter", false)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Application 999 not found");
        verify(followUps, never()).save(any());
    }

    @Test
    void createKeepsTheDoneFlag() {
        when(applications.findById(2L)).thenReturn(Optional.of(application()));
        when(followUps.save(any(FollowUp.class))).thenAnswer(call -> call.getArgument(0));

        FollowUpResponse created = service.create(new FollowUpRequest(2L, DUE, "Send references", true));

        assertThat(created.done()).isTrue();
        assertThat(created.dueDate()).isEqualTo(DUE);
    }

    @Test
    void listFiltersByApplicationAndDone() {
        FollowUp open = new FollowUp(application(), DUE, "Email the recruiter");
        FollowUp finished = new FollowUp(application(), DUE, "Send references");
        finished.setDone(true);
        when(followUps.findByApplicationIdOrderByDueDateAscIdAsc(2L)).thenReturn(List.of(open, finished));

        List<FollowUpResponse> result = service.list(2L, false);

        assertThat(result).extracting(FollowUpResponse::note).containsExactly("Email the recruiter");
    }

    @Test
    void updateChangesDateNoteAndDoneButNeverTheApplication() {
        Application application = application();
        FollowUp followUp = new FollowUp(application, DUE, "Email the recruiter");
        when(followUps.findById(5L)).thenReturn(Optional.of(followUp));

        FollowUpResponse updated = service.update(5,
                new FollowUpUpdateRequest(LocalDate.of(2026, 10, 30), "Call the recruiter", true));

        assertThat(updated.dueDate()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(updated.note()).isEqualTo("Call the recruiter");
        assertThat(updated.done()).isTrue();
        assertThat(followUp.getApplication()).isSameAs(application);
    }
}
