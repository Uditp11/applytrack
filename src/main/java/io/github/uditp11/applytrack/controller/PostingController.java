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

import io.github.uditp11.applytrack.dto.PostingRequest;
import io.github.uditp11.applytrack.dto.PostingResponse;
import io.github.uditp11.applytrack.service.PostingService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/postings")
public class PostingController {

    private final PostingService service;

    public PostingController(PostingService service) {
        this.service = service;
    }

    @GetMapping
    public List<PostingResponse> list(@RequestParam(required = false) Long companyId) {
        return service.list(companyId);
    }

    @GetMapping("/{id}")
    public PostingResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<PostingResponse> create(@Valid @RequestBody PostingRequest request) {
        PostingResponse created = service.create(request);
        return ResponseEntity.created(Locations.of(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PostingResponse update(@PathVariable long id, @Valid @RequestBody PostingRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
