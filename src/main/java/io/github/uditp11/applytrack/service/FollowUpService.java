package io.github.uditp11.applytrack.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.uditp11.applytrack.domain.Application;
import io.github.uditp11.applytrack.domain.FollowUp;
import io.github.uditp11.applytrack.dto.FollowUpRequest;
import io.github.uditp11.applytrack.dto.FollowUpResponse;
import io.github.uditp11.applytrack.dto.FollowUpUpdateRequest;
import io.github.uditp11.applytrack.repository.ApplicationRepository;
import io.github.uditp11.applytrack.repository.FollowUpRepository;

@Service
@Transactional
public class FollowUpService {

    private final FollowUpRepository followUps;
    private final ApplicationRepository applications;

    public FollowUpService(FollowUpRepository followUps, ApplicationRepository applications) {
        this.followUps = followUps;
        this.applications = applications;
    }

    /** Both filters are optional. The done filter runs in memory: one person's follow-ups are a short list. */
    @Transactional(readOnly = true)
    public List<FollowUpResponse> list(Long applicationId, Boolean done) {
        List<FollowUp> found = applicationId == null
                ? followUps.findAll(Sort.by("dueDate", "id"))
                : followUps.findByApplicationIdOrderByDueDateAscIdAsc(applicationId);
        return found.stream()
                .filter(followUp -> done == null || followUp.isDone() == done)
                .map(FollowUpResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FollowUpResponse get(long id) {
        return FollowUpResponse.from(find(id));
    }

    public FollowUpResponse create(FollowUpRequest request) {
        FollowUp followUp = new FollowUp(application(request.applicationId()), request.dueDate(), request.note());
        followUp.setDone(request.done());
        return FollowUpResponse.from(followUps.save(followUp));
    }

    public FollowUpResponse update(long id, FollowUpUpdateRequest request) {
        FollowUp followUp = find(id);
        followUp.setDueDate(request.dueDate());
        followUp.setNote(request.note());
        followUp.setDone(request.done());
        return FollowUpResponse.from(followUp);
    }

    public void delete(long id) {
        followUps.delete(find(id));
    }

    private FollowUp find(long id) {
        return followUps.findById(id).orElseThrow(() -> new NotFoundException("Follow-up", id));
    }

    private Application application(long id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application", id));
    }
}
