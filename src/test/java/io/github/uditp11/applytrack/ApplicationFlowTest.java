package io.github.uditp11.applytrack;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.uditp11.applytrack.domain.ApplicationStatus;
import io.github.uditp11.applytrack.service.ApplicationService;
import io.micrometer.core.instrument.MeterRegistry;

/** The whole stack: real HTTP, real Spring, real PostgreSQL with the Flyway schema. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureObservability // Spring Boot tests switch metrics export off unless asked
@Testcontainers(disabledWithoutDocker = true)
class ApplicationFlowTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ApplicationService applications;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private MeterRegistry meters;

    private JsonNode post(String path, Map<String, ?> body, HttpStatus expected) {
        ResponseEntity<JsonNode> response = http.postForEntity(path, body, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(expected);
        return response.getBody();
    }

    @Test
    void anApplicationMovesThroughItsStatusesAndRefusesAnIllegalMove() {
        long companyId = post("/api/companies",
                Map.of("name", "Fabrikam Robotics", "website", "https://fabrikam.example.com"),
                HttpStatus.CREATED).get("id").asLong();
        long postingId = post("/api/postings",
                Map.of("companyId", companyId, "title", "Java Developer", "location", "Frankfurt"),
                HttpStatus.CREATED).get("id").asLong();
        JsonNode application = post("/api/applications", Map.of("postingId", postingId), HttpStatus.CREATED);
        long id = application.get("id").asLong();
        assertThat(application.get("status").asText()).isEqualTo("DRAFT");

        String transitions = "/api/applications/" + id + "/transitions";
        post(transitions, Map.of("status", "APPLIED"), HttpStatus.OK);
        JsonNode interview = post(transitions, Map.of("status", "INTERVIEW"), HttpStatus.OK);
        assertThat(interview.get("status").asText()).isEqualTo("INTERVIEW");

        ResponseEntity<JsonNode> illegal = http.postForEntity(transitions, Map.of("status", "DRAFT"), JsonNode.class);
        assertThat(illegal.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(illegal.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(illegal.getBody().get("detail").asText())
                .isEqualTo("Cannot move application " + id + " from INTERVIEW to DRAFT");

        JsonNode history = http.getForObject(transitions, JsonNode.class);
        assertThat(history).hasSize(3);
        assertThat(history.get(0).get("from").isNull()).isTrue();
        assertThat(history.get(0).get("to").asText()).isEqualTo("DRAFT");
        assertThat(history.get(2).get("to").asText()).isEqualTo("INTERVIEW");

        JsonNode stats = http.getForObject("/api/stats", JsonNode.class);
        assertThat(stats.get("byStatus")).hasSize(6);
        assertThat(stats.get("byStatus").get("INTERVIEW").asLong()).isPositive();
        assertThat(stats.get("responded").asLong()).isPositive();

        String metrics = http.getForObject("/actuator/prometheus", String.class);
        assertThat(metrics).containsPattern(
                "applytrack_status_transitions_total\\{[^}]*from=\"APPLIED\"[^}]*to=\"INTERVIEW\"[^}]*\\} [1-9]");
    }

    @Test
    void theDatabaseRulesComeBackAs409Problems() {
        long companyId = post("/api/companies", Map.of("name", "Tailspin Media"), HttpStatus.CREATED)
                .get("id").asLong();
        long postingId = post("/api/postings", Map.of("companyId", companyId, "title", "Platform Engineer"),
                HttpStatus.CREATED).get("id").asLong();
        post("/api/applications", Map.of("postingId", postingId), HttpStatus.CREATED);

        JsonNode secondApplication = post("/api/applications", Map.of("postingId", postingId), HttpStatus.CONFLICT);
        assertThat(secondApplication.get("title").asText()).isEqualTo("Conflict");

        post("/api/companies", Map.of("name", "Tailspin Media"), HttpStatus.CONFLICT);

        ResponseEntity<JsonNode> deleteCompany = http.exchange("/api/companies/" + companyId, HttpMethod.DELETE,
                null, JsonNode.class);
        assertThat(deleteCompany.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(http.getForEntity("/api/companies/" + companyId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private double movesCounted(String from, String to) {
        return meters.get("applytrack.status.transitions").tag("from", from).tag("to", to).counter().count();
    }

    @Test
    void onlyCommittedMovesAreCounted() {
        long companyId = post("/api/companies", Map.of("name", "Adventure Works Cycles"), HttpStatus.CREATED)
                .get("id").asLong();
        long postingId = post("/api/postings", Map.of("companyId", companyId, "title", "QA Engineer"),
                HttpStatus.CREATED).get("id").asLong();
        long id = post("/api/applications", Map.of("postingId", postingId), HttpStatus.CREATED).get("id").asLong();
        double before = movesCounted("DRAFT", "APPLIED");

        // The move runs inside an outer transaction that is then rolled back, as if the commit had failed.
        transactions.executeWithoutResult(status -> {
            applications.moveTo(id, ApplicationStatus.APPLIED);
            status.setRollbackOnly();
        });
        assertThat(applications.get(id).status()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(movesCounted("DRAFT", "APPLIED")).isEqualTo(before);

        applications.moveTo(id, ApplicationStatus.APPLIED);
        assertThat(movesCounted("DRAFT", "APPLIED")).isEqualTo(before + 1);
    }

    @Test
    void theOpenApiDocumentNamesTheApi() {
        JsonNode docs = http.getForObject("/v3/api-docs", JsonNode.class);

        assertThat(docs.get("info").get("title").asText()).isEqualTo("ApplyTrack API");
        assertThat(docs.get("paths").has("/api/applications/{id}/transitions")).isTrue();
    }
}
