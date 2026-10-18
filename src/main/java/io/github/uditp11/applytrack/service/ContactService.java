package io.github.uditp11.applytrack.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.uditp11.applytrack.domain.Company;
import io.github.uditp11.applytrack.domain.Contact;
import io.github.uditp11.applytrack.dto.ContactRequest;
import io.github.uditp11.applytrack.dto.ContactResponse;
import io.github.uditp11.applytrack.repository.CompanyRepository;
import io.github.uditp11.applytrack.repository.ContactRepository;

@Service
@Transactional
public class ContactService {

    private final ContactRepository contacts;
    private final CompanyRepository companies;

    public ContactService(ContactRepository contacts, CompanyRepository companies) {
        this.contacts = contacts;
        this.companies = companies;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> list(Long companyId) {
        List<Contact> found = companyId == null
                ? contacts.findAll(Sort.by("id"))
                : contacts.findByCompanyIdOrderById(companyId);
        return found.stream().map(ContactResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ContactResponse get(long id) {
        return ContactResponse.from(find(id));
    }

    public ContactResponse create(ContactRequest request) {
        Contact contact = new Contact(company(request.companyId()), request.name(), request.email(), request.role());
        return ContactResponse.from(contacts.save(contact));
    }

    public ContactResponse update(long id, ContactRequest request) {
        Contact contact = find(id);
        contact.setCompany(company(request.companyId()));
        contact.setName(request.name());
        contact.setEmail(request.email());
        contact.setRole(request.role());
        return ContactResponse.from(contact);
    }

    public void delete(long id) {
        contacts.delete(find(id));
    }

    private Contact find(long id) {
        return contacts.findById(id).orElseThrow(() -> new NotFoundException("Contact", id));
    }

    private Company company(long id) {
        return companies.findById(id).orElseThrow(() -> new NotFoundException("Company", id));
    }
}
