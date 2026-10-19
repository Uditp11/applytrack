package io.github.uditp11.applytrack;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "ApplyTrack API",
        version = "0.1.0",
        description = "Track job applications: companies, postings, contacts, applications with a status "
                + "state machine, follow-ups and statistics. All sample data is fictional."))
public class ApplyTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplyTrackApplication.class, args);
    }

    /**
     * One source of "now", so tests can replace it with a fixed clock. It ticks in microseconds because
     * that is what PostgreSQL stores, so a response shows the same time before and after a reload.
     */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
