package de.thokari.epages.app.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
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
        .put("return_url", "http://localhost:8080/epages-app")
        .put("signature", "khbDPOK6OWAk4u+XGOkcy6b30LanJc6Y+Q2AHnFtxu8=");

    InstallationRequest installationRequest;

    @Before
    public void setup() {
        installationRequest = Model.fromJsonObject(source, InstallationRequest.class);
    }

    @Test
    public void testCalculatesCorrectTokenPath() {
        assertEquals("/token", installationRequest.tokenPath);
    }

    @Test
    public void testCanBeSerialized() {
        source.put("token_path", "/token");
        assertEquals(source.encode(), installationRequest.toJsonObject().encode());
    }

    @Test
    public void testValidatesCorrectSignature() {
        String secret = "my-client-secret";
        installationRequest = Model.fromJsonObject(source, InstallationRequest.class);
        assertTrue(installationRequest.hasValidSignature(secret));
    }

    @Test
    public void testFailsOnWrongSignature() {
        String secret = "my-client-secret";
        source.put("signature", "invalid_signature");
        installationRequest = Model.fromJsonObject(source, InstallationRequest.class);
        assertFalse(installationRequest.hasValidSignature(secret));
    }

}
