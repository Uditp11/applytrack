package io.github.uditp11.applytrack.controller;

import java.net.URI;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Builds the Location header of a 201 response: the current URL plus the new id. */
final class Locations {

    private Locations() {
    }

    static URI of(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    }
}
