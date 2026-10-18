package io.github.uditp11.applytrack.dto;

import java.time.LocalDate;

import io.github.uditp11.applytrack.domain.FollowUp;

public record FollowUpResponse(Long id, Long applicationId, LocalDate dueDate, String note, boolean done) {

    public static FollowUpResponse from(FollowUp followUp) {
        return new FollowUpResponse(followUp.getId(), followUp.getApplication().getId(), followUp.getDueDate(),
                followUp.getNote(), followUp.isDone());
    }
}
