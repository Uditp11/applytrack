package io.github.uditp11.applytrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.Contact;
import io.github.uditp11.applytrack.dto.ContactRequest;
import io.github.uditp11.applytrack.dto.ContactResponse;
import io.github.uditp11.applytrack.repository.CompanyRepository;
import io.github.uditp11.applytrack.repository.ContactRepository;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contacts;

    @Mock
    private CompanyRepository companies;

    @InjectMocks
    private ContactService service;

    @Test
    void createForAnUnknownCompanyThrowsNotFoundAndSavesNothing() {
        when(companies.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new ContactRequest(999L, "Mira Okafor", null, "Recruiter")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company 999 not found");
        verify(contacts, never()).save(any());
    }

    @Test
    void listWithACompanyIdReturnsOnlyThatCompanysContacts() {
        Company company = new Company("Contoso Freight", null, Instant.parse("2026-10-18T09:00:00Z"));
        when(contacts.findByCompanyIdOrderById(4L)).thenReturn(
                List.of(new Contact(company, "Jonas Weber", "jonas.weber@example.com", "Hiring manager")));

        List<ContactResponse> result = service.list(4L);

        assertThat(result).extracting(ContactResponse::name).containsExactly("Jonas Weber");
        verify(contacts, never()).findAll(any(Sort.class));
    }
}
