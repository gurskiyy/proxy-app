package com.gurskiyy.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class ProxyResourceIntegrationTest {

    @Test
    void shouldReturnDefaultProxyPage() {
        given()
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("Quarkus"));
    }

    @Test
    void shouldReturnTargetProxyPage() {
        given()
                .when()
                .get("/?target=https://pt.quarkus.io")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("<html"))
                .body(containsString("pt.quarkus.io"));
    }

    @Test
    void shouldReturn500ForInvalidTarget() {
        given()
                .when()
                .get("/?target=https://invalid.quarkus.io")
                .then()
                .statusCode(500)
                .body(containsString("Error forwarding request"));
    }
}
