package com.bookstore.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/** Real embedded Tomcat HTTP tests; no MockMvc and no mocked request executor. */
class CloudApiTest {
    static final String TEST_SECRET = "test-only-signing-secret-at-least-32-bytes-123456";
    static ConfigurableApplicationContext context;
    static String base;
    static ObjectMapper mapper;
    static HttpClient client;
    static String token;

    @BeforeAll
    static void start() throws Exception {
        context = new SpringApplicationBuilder(CloudApplication.class).run(
                "--server.port=0", "--jwt.secret=" + TEST_SECRET,
                "--bookstore.io-delay-ms=10", "--spring.threads.virtual.enabled=true");
        base = "http://127.0.0.1:" + ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        mapper = context.getBean(ObjectMapper.class);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var login = post("/api/auth/token", "{\"username\":\"student\",\"password\":\"demo-password\"}");
        assertEquals(200, login.statusCode());
        token = mapper.readTree(login.body()).at("/data/accessToken").asText();
        assertFalse(token.isBlank());
    }

    @AfterAll
    static void stop() { if (context != null) context.close(); }

    static HttpResponse<String> get(String path, String authorization) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(10)).GET();
        if (authorization != null) request.header("Authorization", authorization);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    static void assertError(HttpResponse<String> response, int status) throws Exception {
        assertEquals(status, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(status, body.get("code").asInt());
        assertTrue(body.get("data").isNull());
    }

    @Test void missingTokenReturns401() throws Exception { assertError(get("/api/books/1", null), 401); }
    @Test void malformedTokenReturns401() throws Exception { assertError(get("/api/books/1", "Bearer not-a-jwt"), 401); }
    @Test void emptyBearerReturns401() throws Exception { assertError(get("/api/books/1", "Bearer "), 401); }
    @Test void basicAuthIsNotAccepted() throws Exception { assertError(get("/api/books/1", "Basic Zm9vOmJhcg=="), 401); }

    @Test void validTokenReturnsBookAndVirtualRequestThread() throws Exception {
        var result = get("/api/books/1", "Bearer " + token);
        assertEquals(200, result.statusCode());
        assertEquals("三体", mapper.readTree(result.body()).at("/data/title").asText());
        assertEquals("59.80", mapper.readTree(result.body()).at("/data/price").asText());
        assertEquals("true", result.headers().firstValue("X-Request-Thread-Virtual").orElseThrow());
        assertTrue(result.headers().firstValue("X-Request-Thread-Name").orElseThrow().startsWith("tomcat-handler-"));
        assertTrue(result.headers().allValues("Set-Cookie").isEmpty());
    }

    @Test void lowercaseBearerSchemeIsAccepted() throws Exception {
        assertEquals(200, get("/api/books/1", "bearer " + token).statusCode());
    }

    @Test void tamperedSignatureIsRejected() throws Exception {
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? "B" : "A") + parts[2].substring(1);
        assertError(get("/api/books/1", "Bearer " + String.join(".", parts)), 401);
    }

    String signed(String issuer, Instant expiry, String subject, String role) {
        return Jwts.builder().issuer(issuer).subject(subject).claim("role", role)
                .issuedAt(Date.from(Instant.now().minusSeconds(10)))
                .expiration(expiry == null ? null : Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }

    @Test void expiredTokenReturns401() throws Exception {
        assertError(get("/api/books/1", "Bearer " + signed("ebookstore-cloud", Instant.now().minusSeconds(5), "student", "USER")), 401);
    }
    @Test void wrongIssuerReturns401() throws Exception {
        assertError(get("/api/books/1", "Bearer " + signed("wrong-issuer", Instant.now().plusSeconds(60), "student", "USER")), 401);
    }
    @Test void missingExpirationReturns401() throws Exception {
        assertError(get("/api/books/1", "Bearer " + signed("ebookstore-cloud", null, "student", "USER")), 401);
    }
    @Test void missingSubjectReturns401() throws Exception {
        assertError(get("/api/books/1", "Bearer " + signed("ebookstore-cloud", Instant.now().plusSeconds(60), null, "USER")), 401);
    }
    @Test void invalidRoleReturns401() throws Exception {
        assertError(get("/api/books/1", "Bearer " + signed("ebookstore-cloud", Instant.now().plusSeconds(60), "student", "ADMIN")), 401);
    }
    @Test void wrongCredentialsReturn401() throws Exception {
        assertError(post("/api/auth/token", "{\"username\":\"student\",\"password\":\"wrong\"}"), 401);
    }
    @Test void missingLoginFieldsReturn400() throws Exception {
        assertError(post("/api/auth/token", "{\"username\":\"student\"}"), 400);
    }
    @Test void malformedLoginJsonReturns400() throws Exception { assertError(post("/api/auth/token", "{"), 400); }
    @Test void invalidBookIdReturns400() throws Exception { assertError(get("/api/books/0", "Bearer " + token), 400); }
    @Test void nonNumericBookIdReturns400() throws Exception { assertError(get("/api/books/abc", "Bearer " + token), 400); }
    @Test void absentBookReturns404() throws Exception { assertError(get("/api/books/99999", "Bearer " + token), 404); }

    @Test void authIsNotRememberedAcrossRequests() throws Exception {
        assertEquals(200, get("/api/books/1", "Bearer " + token).statusCode());
        assertError(get("/api/books/1", null), 401);
    }
    @Test void sessionCookieDoesNotAuthenticate() throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + "/api/books/1"))
                .header("Cookie", "JSESSIONID=not-a-real-session").GET().build();
        assertError(client.send(request, HttpResponse.BodyHandlers.ofString()), 401);
    }

    @Test void tokenIssueResponseIsNotCached() throws Exception {
        var response = post("/api/auth/token", "{\"username\":\"student\",\"password\":\"demo-password\"}");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));
        var claims = context.getBean(JwtService.class).verify(mapper.readTree(response.body()).at("/data/accessToken").asText());
        assertEquals("student", claims.getSubject());
        assertEquals("USER", claims.get("role"));
    }

    @Test void concurrentHttpRequestsAllUseVirtualThreads() {
        var pending = new ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int i = 0; i < 40; i++) {
            var request = HttpRequest.newBuilder(URI.create(base + "/api/books/1"))
                    .header("Authorization", "Bearer " + token).timeout(Duration.ofSeconds(10)).GET().build();
            pending.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
        }
        for (var future : pending) {
            var response = future.join();
            assertEquals(200, response.statusCode());
            assertEquals("true", response.headers().firstValue("X-Request-Thread-Virtual").orElseThrow());
            assertTrue(response.headers().allValues("Set-Cookie").isEmpty());
        }
    }
}
