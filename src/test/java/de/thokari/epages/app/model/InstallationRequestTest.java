package de.thokari.epages.app.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.vertx.core.json.JsonObject;

@RunWith(JUnit4.class)
public class InstallationRequestTest {

    final JsonObject source = new JsonObject()
        .put("code", "f32ddSbuff2IGAYvtiwYQiyHyuLJWbey")
        .put("api_url", "http://localhost:9999/api")
        .put("access_token_url", "http://localhost:9999/api/token")
        .put("return_url", "http://localhost:8080/epages-app");

    final InstallationRequest installationEvent = Model.fromJsonObject(source, InstallationRequest.class);

    @Test
    public void testCalculatesCorrectTokenPath() {
        assertEquals("/token", installationEvent.tokenPath);
    }

    @Test
    public void testCanBeSerialized() {
        JsonObject expected = source.put("token_path", "/token");
        assertEquals(expected.encode(), installationEvent.toJsonObject().encode());
    }
    
}
