package io.github.uditp11.applytrack;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApplyTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplyTrackApplication.class, args);
    }

    /** One source of "now", so tests can replace it with a fixed clock. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
