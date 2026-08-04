package org.coffee;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class MenuResourceTest {

    @Test
    void testGetMenuReturnsItems() {
        given()
            .when().get("/menu")
            .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void testPostMenuAddsItem() {
        String newItem = """
            {"name":"Latte","description":"Smooth espresso with milk","price":4.25}
            """;
        given()
            .contentType("application/json")
            .body(newItem)
            .when().post("/menu")
            .then()
                .statusCode(201)
                .body("name", is("Latte"));
    }
}
