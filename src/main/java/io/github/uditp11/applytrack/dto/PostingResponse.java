package io.github.uditp11.applytrack.dto;

import java.time.Instant;

import io.github.uditp11.applytrack.domain.Posting;

public record PostingResponse(Long id, Long companyId, String companyName, String title, String location,
        String url, Instant createdAt) {

    public static PostingResponse from(Posting posting) {
        return new PostingResponse(posting.getId(), posting.getCompany().getId(), posting.getCompany().getName(),
                posting.getTitle(), posting.getLocation(), posting.getUrl(), posting.getCreatedAt());
    }
}
