package io.github.uditp11.applytrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.uditp11.applytrack.domain.Contact;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByCompanyIdOrderById(long companyId);
}
