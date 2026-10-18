package io.github.uditp11.applytrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotNull Long companyId,
        @NotBlank @Size(max = 200) String name,
        @Email @Size(max = 320) String email,
        @Size(max = 200) String role) {
}
