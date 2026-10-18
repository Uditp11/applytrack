package io.github.uditp11.applytrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.uditp11.applytrack.domain.Posting;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    List<Posting> findByCompanyIdOrderById(long companyId);
}
