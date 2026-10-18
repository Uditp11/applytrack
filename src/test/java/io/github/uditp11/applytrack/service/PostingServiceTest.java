package io.github.uditp11.applytrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.Posting;
import io.github.uditp11.applytrack.dto.PostingRequest;
import io.github.uditp11.applytrack.dto.PostingResponse;
import io.github.uditp11.applytrack.repository.CompanyRepository;
import io.github.uditp11.applytrack.repository.PostingRepository;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");

    @Mock
    private PostingRepository postings;

    @Mock
    private CompanyRepository companies;

    private PostingService service;

    @BeforeEach
    void setUp() {
        service = new PostingService(postings, companies, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createForAnUnknownCompanyThrowsNotFoundAndSavesNothing() {
        when(companies.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new PostingRequest(999L, "Backend Developer", "Frankfurt", null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company 999 not found");
        verify(postings, never()).save(any());
    }

    @Test
    void createLinksThePostingToItsCompany() {
        Company company = new Company("Northwind Analytics", null, NOW);
        when(companies.findById(1L)).thenReturn(Optional.of(company));
        when(postings.save(any(Posting.class))).thenAnswer(call -> call.getArgument(0));

        PostingResponse created = service.create(
                new PostingRequest(1L, "Backend Developer", "Frankfurt", "https://jobs.example.com/42"));

        assertThat(created.companyName()).isEqualTo("Northwind Analytics");
        assertThat(created.title()).isEqualTo("Backend Developer");
        assertThat(created.createdAt()).isEqualTo(NOW);
    }
}
