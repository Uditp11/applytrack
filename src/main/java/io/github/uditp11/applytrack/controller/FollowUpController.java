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

import io.github.uditp11.applytrack.dto.FollowUpRequest;
import io.github.uditp11.applytrack.dto.FollowUpResponse;
import io.github.uditp11.applytrack.dto.FollowUpUpdateRequest;
import io.github.uditp11.applytrack.service.FollowUpService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    private final FollowUpService service;

    public FollowUpController(FollowUpService service) {
        this.service = service;
    }

    @GetMapping
    public List<FollowUpResponse> list(@RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Boolean done) {
        return service.list(applicationId, done);
    }

    @GetMapping("/{id}")
    public FollowUpResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<FollowUpResponse> create(@Valid @RequestBody FollowUpRequest request) {
        FollowUpResponse created = service.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public FollowUpResponse update(@PathVariable long id, @Valid @RequestBody FollowUpUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
