package io.github.uditp11.applytrack;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class HealthEndpointTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void healthEndpointReportsUpIncludingTheDatabase() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
        assertThat(response.getBody()).contains("\"db\":{\"status\":\"UP\"");
    }

    /** Kubernetes restarts a pod whose liveness fails, so liveness must not depend on the database. */
    @Test
    void livenessIsUpAndDoesNotIncludeTheDatabase() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health/liveness", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"").doesNotContain("\"db\"");
    }

    /** Kubernetes stops sending traffic to a pod whose readiness fails, so readiness checks the database. */
    @Test
    void readinessIsUpAndIncludesTheDatabase() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"").contains("\"db\":{\"status\":\"UP\"");
    }
}
