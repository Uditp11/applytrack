package io.github.uditp11.applytrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.dto.ApplicationResponse;
import io.github.uditp11.applytrack.service.ApplicationService;
import io.github.uditp11.applytrack.service.IllegalTransitionException;
import io.github.uditp11.applytrack.service.NotFoundException;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApplicationService service;

    private static final String TRANSITIONS = "/api/applications/7/transitions";

    @Test
    void aLegalMoveReturnsTheUpdatedApplication() throws Exception {
        Instant now = Instant.parse("2026-10-18T09:00:00Z");
        when(service.moveTo(7, ApplicationStatus.APPLIED)).thenReturn(new ApplicationResponse(7L, 3L,
                "Backend Developer", "Northwind Analytics", ApplicationStatus.APPLIED, null, now, now));

        mvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void anIllegalMoveIsA409ProblemNamingBothStatuses() throws Exception {
        when(service.moveTo(7, ApplicationStatus.DRAFT))
                .thenThrow(new IllegalTransitionException(7, ApplicationStatus.INTERVIEW, ApplicationStatus.DRAFT));

        mvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Illegal status transition"))
                .andExpect(jsonPath("$.detail").value("Cannot move application 7 from INTERVIEW to DRAFT"));
    }

    @Test
    void anUnknownStatusValueIsA400Problem() throws Exception {
        mvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HIRED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(service);
    }

    @Test
    void aMissingStatusIsA400ProblemNamingTheField() throws Exception {
        mvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void malformedJsonIsA400Problem() throws Exception {
        mvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON).content("{\"status\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void movingAnUnknownApplicationIsA404Problem() throws Exception {
        when(service.moveTo(eq(7L), any())).thenThrow(new NotFoundException("Application", 7));

        mvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Application 7 not found"));
    }

    @Test
    void aChangeMadeAtTheSameTimeIsA409Problem() throws Exception {
        when(service.moveTo(7, ApplicationStatus.APPLIED))
                .thenThrow(new ObjectOptimisticLockingFailureException("Application", 7L));

        mvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }
}
