package io.github.uditp11.applytrack.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.domain.Posting;
import io.github.uditp11.applytrack.domain.StatusTransition;
import io.github.uditp11.applytrack.dto.ApplicationCreateRequest;
import io.github.uditp11.applytrack.dto.ApplicationResponse;
import io.github.uditp11.applytrack.dto.ApplicationUpdateRequest;
import io.github.uditp11.applytrack.dto.TransitionResponse;
import io.github.uditp11.applytrack.repository.ApplicationRepository;
import io.github.uditp11.applytrack.repository.PostingRepository;
import io.github.uditp11.applytrack.repository.StatusTransitionRepository;

/** Applications and their status. Every status change goes through {@link #moveTo}. */
@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository applications;
    private final PostingRepository postings;
    private final StatusTransitionRepository transitions;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applications, PostingRepository postings,
            StatusTransitionRepository transitions, Clock clock) {
        this.applications = applications;
        this.postings = postings;
        this.transitions = transitions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(ApplicationStatus status) {
        List<Application> found = status == null
                ? applications.findAll(Sort.by("id"))
                : applications.findByStatusOrderById(status);
        return found.stream().map(ApplicationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(long id) {
        return ApplicationResponse.from(find(id));
    }

    /** New applications start in DRAFT; that first status is recorded as a transition from nothing. */
    public ApplicationResponse create(ApplicationCreateRequest request) {
        Posting posting = postings.findById(request.postingId())
                .orElseThrow(() -> new NotFoundException("Posting", request.postingId()));
        Instant now = clock.instant();
        Application application = applications.save(new Application(posting, request.notes(), now));
        transitions.save(new StatusTransition(application, null, application.getStatus(), now));
        return ApplicationResponse.from(application);
    }

    public ApplicationResponse update(long id, ApplicationUpdateRequest request) {
        Application application = find(id);
        application.changeNotes(request.notes(), clock.instant());
        return ApplicationResponse.from(application);
    }

    public void delete(long id) {
        applications.delete(find(id));
    }

    public ApplicationResponse moveTo(long id, ApplicationStatus target) {
        Application application = find(id);
        ApplicationStatus from = application.getStatus();
        if (!from.canMoveTo(target)) {
            throw new IllegalTransitionException(id, from, target);
        }
        Instant now = clock.instant();
        application.changeStatus(target, now);
        transitions.save(new StatusTransition(application, from, target, now));
        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public List<TransitionResponse> history(long id) {
        find(id);
        return transitions.findByApplicationIdOrderByOccurredAtAscIdAsc(id).stream()
                .map(TransitionResponse::from).toList();
    }

    private Application find(long id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application", id));
    }
}
