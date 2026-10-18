package io.github.uditp11.applytrack;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
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
