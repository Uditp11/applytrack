package io.github.uditp11.applytrack.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.uditp11.applytrack.dto.StatsResponse;
import io.github.uditp11.applytrack.service.StatsService;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @GetMapping
    public StatsResponse stats() {
        return service.stats();
    }
}
