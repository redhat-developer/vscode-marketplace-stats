package com.redhat.devtools.stats.endpoints;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class MarketPlaceProxyTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/api/redhat.java")
          .then()
             .statusCode(200)
             .body(containsString("Language Support for Java(TM) by Red Hat"));
    }

}