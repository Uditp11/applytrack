package io.github.uditp11.applytrack.service;

import java.time.Clock;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.dto.CompanyRequest;
import io.github.uditp11.applytrack.dto.CompanyResponse;
import io.github.uditp11.applytrack.repository.CompanyRepository;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companies;
    private final Clock clock;

    public CompanyService(CompanyRepository companies, Clock clock) {
        this.companies = companies;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list() {
        return companies.findAll(Sort.by("name")).stream().map(CompanyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(long id) {
        return CompanyResponse.from(find(id));
    }

    public CompanyResponse create(CompanyRequest request) {
        Company company = new Company(request.name(), request.website(), clock.instant());
        return CompanyResponse.from(companies.save(company));
    }

    public CompanyResponse update(long id, CompanyRequest request) {
        Company company = find(id);
        company.setName(request.name());
        company.setWebsite(request.website());
        companies.flush(); // surface a duplicate name here, as a DataIntegrityViolationException
        return CompanyResponse.from(company);
    }

    /** Fails with a conflict while postings or contacts still belong to the company. */
    public void delete(long id) {
        companies.delete(find(id));
        companies.flush();
    }

    Company find(long id) {
        return companies.findById(id).orElseThrow(() -> new NotFoundException("Company", id));
    }
}
