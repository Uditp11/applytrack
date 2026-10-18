package io.github.uditp11.applytrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.uditp11.applytrack.dto.CompanyRequest;
import io.github.uditp11.applytrack.dto.CompanyResponse;
import io.github.uditp11.applytrack.service.CompanyService;
import io.github.uditp11.applytrack.service.NotFoundException;

/** Only the web layer: the service is a Mockito mock, no database. */
@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CompanyService service;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any(CompanyRequest.class))).thenReturn(
                new CompanyResponse(5L, "Northwind Analytics", null, Instant.parse("2026-10-18T09:00:00Z")));

        mvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Northwind Analytics\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/companies/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void aBlankNameIsRejectedWithTheFieldNamed() throws Exception {
        mvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void anUnknownIdIsA404Problem() throws Exception {
        when(service.get(7)).thenThrow(new NotFoundException("Company", 7));

        mvc.perform(get("/api/companies/7"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("Company 7 not found"));
    }

    @Test
    void aDeleteBlockedByTheDatabaseIsA409ProblemWithoutSqlDetails() throws Exception {
        doThrow(new DataIntegrityViolationException("violates foreign key constraint posting_company_id_fkey"))
                .when(service).delete(3);

        mvc.perform(delete("/api/companies/3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("constraint"))));
    }
}
