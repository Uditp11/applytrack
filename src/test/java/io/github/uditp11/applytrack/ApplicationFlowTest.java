package io.github.uditp11.applytrack;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;

/** The whole stack: real HTTP, real Spring, real PostgreSQL with the Flyway schema. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplicationFlowTest {

    @Autowired
    private TestRestTemplate http;

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
}
