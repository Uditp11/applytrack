package io.github.uditp11.applytrack.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.dto.ApplicationCreateRequest;
import io.github.uditp11.applytrack.dto.ApplicationResponse;
import io.github.uditp11.applytrack.dto.ApplicationUpdateRequest;
import io.github.uditp11.applytrack.dto.TransitionRequest;
import io.github.uditp11.applytrack.dto.TransitionResponse;
import io.github.uditp11.applytrack.service.ApplicationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicationResponse> list(@RequestParam(required = false) ApplicationStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationCreateRequest request) {
        ApplicationResponse created = service.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(@PathVariable long id, @Valid @RequestBody ApplicationUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Moves the application to a new status. Answers 409 if the state machine does not allow it. */
    @PostMapping("/{id}/transitions")
    public ApplicationResponse move(@PathVariable long id, @Valid @RequestBody TransitionRequest request) {
        return service.moveTo(id, request.status());
    }

    @GetMapping("/{id}/transitions")
    public List<TransitionResponse> history(@PathVariable long id) {
        return service.history(id);
    }
}
