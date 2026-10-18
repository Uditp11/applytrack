package io.github.uditp11.applytrack.dto;

import io.github.uditp11.applytrack.domain.Contact;

public record ContactResponse(Long id, Long companyId, String name, String email, String role) {

    public static ContactResponse from(Contact contact) {
        return new ContactResponse(contact.getId(), contact.getCompany().getId(), contact.getName(),
                contact.getEmail(), contact.getRole());
    }
}
