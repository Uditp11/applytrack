package io.github.uditp11.applytrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import io.github.uditp11.applytrack.dto.CompanyRequest;
import io.github.uditp11.applytrack.dto.CompanyResponse;
import io.github.uditp11.applytrack.repository.CompanyRepository;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T09:00:00Z");

    @Mock
    private CompanyRepository companies;

    private CompanyService service;

    @BeforeEach
    void setUp() {
        service = new CompanyService(companies, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getOfAnUnknownIdThrowsNotFound() {
        when(companies.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(7))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Company 7 not found");
    }

    @Test
    void createSavesTheCompanyWithTheCurrentTime() {
        when(companies.save(any(Company.class))).thenAnswer(call -> call.getArgument(0));

        CompanyResponse created = service.create(new CompanyRequest("Northwind Analytics", "https://northwind.example.com"));

        assertThat(created.name()).isEqualTo("Northwind Analytics");
        assertThat(created.website()).isEqualTo("https://northwind.example.com");
        assertThat(created.createdAt()).isEqualTo(NOW);
    }

    @Test
    void updateChangesNameAndWebsite() {
        Company existing = new Company("Northwind", null, NOW);
        when(companies.findById(3L)).thenReturn(Optional.of(existing));

        CompanyResponse updated = service.update(3, new CompanyRequest("Northwind Analytics", "https://northwind.example.com"));

        assertThat(updated.name()).isEqualTo("Northwind Analytics");
        assertThat(existing.getWebsite()).isEqualTo("https://northwind.example.com");
    }
}
