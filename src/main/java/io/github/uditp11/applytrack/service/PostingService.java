package io.github.uditp11.applytrack.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.Posting;
import io.github.uditp11.applytrack.dto.PostingRequest;
import io.github.uditp11.applytrack.dto.PostingResponse;
import io.github.uditp11.applytrack.repository.CompanyRepository;
import io.github.uditp11.applytrack.repository.PostingRepository;

@Service
@Transactional
public class PostingService {

    private final PostingRepository postings;
    private final CompanyRepository companies;
    private final Clock clock;

    public PostingService(PostingRepository postings, CompanyRepository companies, Clock clock) {
        this.postings = postings;
        this.companies = companies;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PostingResponse> list(Long companyId) {
        List<Posting> found = companyId == null ? postings.findAll() : postings.findByCompanyId(companyId);
        return found.stream().map(PostingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PostingResponse get(long id) {
        return PostingResponse.from(find(id));
    }

    public PostingResponse create(PostingRequest request) {
        Posting posting = new Posting(company(request.companyId()), request.title(), request.location(),
                request.url(), clock.instant());
        return PostingResponse.from(postings.save(posting));
    }

    public PostingResponse update(long id, PostingRequest request) {
        Posting posting = find(id);
        posting.setCompany(company(request.companyId()));
        posting.setTitle(request.title());
        posting.setLocation(request.location());
        posting.setUrl(request.url());
        return PostingResponse.from(posting);
    }

    /** Fails with a conflict while an application still points at the posting. */
    public void delete(long id) {
        postings.delete(find(id));
        postings.flush();
    }

    private Posting find(long id) {
        return postings.findById(id).orElseThrow(() -> new NotFoundException("Posting", id));
    }

    private Company company(long id) {
        return companies.findById(id).orElseThrow(() -> new NotFoundException("Company", id));
    }
}
