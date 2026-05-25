package com.challenge.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"app.security.username=erus", "app.security.password=changeme"})
class ApplicationSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedGetReturns401WithWwwAuthenticate() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/employee", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE))
                .as("WWW-Authenticate header must be present on 401")
                .isNotBlank();
    }

    @Test
    void authenticatedGetReturns200() {
        ResponseEntity<String> response =
                restTemplate.withBasicAuth("erus", "changeme").getForEntity("/api/v1/employee", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        if (response.getBody() != null && !response.getBody().isBlank()) {
            assertThat(response.getBody().trim()).startsWith("[");
        }
    }

    @Test
    void postThenGetByUuidRoundTrip() throws Exception {
        Map<String, Object> payload = Map.of(
                "firstName", "Round",
                "lastName", "Trip",
                "salary", 99_000,
                "age", 41,
                "jobTitle", "Smoke Tester",
                "email", "round.trip@example.com",
                "contractHireDate", Instant.parse("2021-03-01T08:00:00Z").toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

        ResponseEntity<String> createResponse = restTemplate
                .withBasicAuth("erus", "changeme")
                .exchange("/api/v1/employee", HttpMethod.POST, request, String.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = createResponse.getHeaders().getLocation();
        assertThat(location).as("Location header must be present").isNotNull();

        UUID createdUuid = extractUuid(createResponse.getBody(), location);
        assertThat(createdUuid).isNotNull();

        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("erus", "changeme")
                .getForEntity("/api/v1/employee/{uuid}", String.class, createdUuid);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(getResponse.getBody());
        assertThat(body.get("uuid").asText()).isEqualTo(createdUuid.toString());
        assertThat(body.get("firstName").asText()).isEqualTo("Round");
        assertThat(body.get("lastName").asText()).isEqualTo("Trip");
        assertThat(body.get("fullName").asText()).isEqualTo("Round Trip");
        assertThat(body.get("salary").asInt()).isEqualTo(99_000);
        assertThat(body.get("age").asInt()).isEqualTo(41);
        assertThat(body.get("jobTitle").asText()).isEqualTo("Smoke Tester");
        assertThat(body.get("email").asText()).isEqualTo("round.trip@example.com");
    }

    private UUID extractUuid(String body, URI location) throws Exception {
        if (body != null && !body.isBlank()) {
            JsonNode node = objectMapper.readTree(body);
            JsonNode uuidNode = node.get("uuid");
            if (uuidNode != null && !uuidNode.isNull()) {
                return UUID.fromString(uuidNode.asText());
            }
        }
        String path = location.getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }
}
