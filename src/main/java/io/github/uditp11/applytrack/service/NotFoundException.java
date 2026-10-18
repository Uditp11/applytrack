package io.github.uditp11.applytrack.service;

/** Thrown when a request names an id that does not exist. Answered with HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, long id) {
        super(resource + " " + id + " not found");
    }
}
