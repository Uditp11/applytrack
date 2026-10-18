package io.github.uditp11.applytrack.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import io.github.uditp11.applytrack.domain.Posting;

/** List queries join the company, because the response shows the company name. */
public interface PostingRepository extends JpaRepository<Posting, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<Posting> findAll(Sort sort);

    @EntityGraph(attributePaths = "company")
    List<Posting> findByCompanyIdOrderById(long companyId);
}
