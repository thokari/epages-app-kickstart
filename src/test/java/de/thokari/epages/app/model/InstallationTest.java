package de.thokari.epages.app.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.vertx.core.json.JsonArray;

@RunWith(JUnit4.class)
public class InstallationTest {

    final String shopName = "my-shop";
    final String apiUrl = "http://example.com/my-shop";
    final String accessToken = "accessToken1234";
    final Boolean emailConfirmed = false;

    @Test
    public void testGetInsertQuery() {
        Installation installation = new Installation(apiUrl, accessToken, shopName);
        String query = installation.getInsertQuery();

        assertEquals(
            "INSERT INTO installations (api_url, access_token, shop_name, created) VALUES (?, ?, ?, ?)",
            query);
    }

    @Test
    public void testGetInsertQueryParams() {
        Installation installation = new Installation(apiUrl, accessToken, shopName);

        JsonArray expected = new JsonArray()
            .add(apiUrl)
            .add(accessToken)
            .add(shopName);

        JsonArray actual = installation.getInsertQueryParams();
        actual.remove(3); // remove the timestamp parameter to avoid mocking the clock...

        assertEquals(expected, actual);
    }

}
