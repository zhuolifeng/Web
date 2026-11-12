package com.bookstore.cloud;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

/** Shows the evidence header changes when virtual threads are actually disabled. */
class PlatformThreadControlTest {
    @Test void disabledVirtualThreadsUsePlatformRequestThread() throws Exception {
        try (var context = new SpringApplicationBuilder(CloudApplication.class).run(
                "--server.port=0", "--jwt.secret=" + CloudApiTest.TEST_SECRET,
                "--bookstore.io-delay-ms=0", "--spring.threads.virtual.enabled=false")) {
            int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
            String token = context.getBean(JwtService.class).issue("student");
            var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + "/api/books/1"))
                    .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("false", response.headers().firstValue("X-Request-Thread-Virtual").orElseThrow());
        }
    }
}
