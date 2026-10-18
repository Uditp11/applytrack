package io.github.uditp11.applytrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostingRequest(
        @NotNull Long companyId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String location,
        @Size(max = 500) String url) {
}
