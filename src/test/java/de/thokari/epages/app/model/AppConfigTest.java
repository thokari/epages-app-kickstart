package de.thokari.epages.app.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

@RunWith(JUnit4.class)
public class AppConfigTest {

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    private static JsonObject configJson;

    @BeforeClass
    public static void readConfig() throws IOException {
        configJson = new JsonObject(new String(Files.readAllBytes(Paths.get("config.test.json"))));
    }

    @Test
    public void testValidConfigCanBeRead() {
        AppConfig appConfig = Json.decodeValue(configJson.encode(), AppConfig.class);
        System.out.println(appConfig.toJsonObject().encodePrettily());
        assertEquals(configJson, appConfig.toJsonObject());
    }

    @Test
    public void testOverrideFromEnvironmentWorks() {
        environment.set("CLIENT_ID", "client-id-from-env");
        environment.set("DB_USERNAME", "db-user-from-env");
        JsonObject modified = configJson.copy();
        modified.remove("clientId");
        modified.getJsonObject("database").remove("username");

        AppConfig appConfig = Json.decodeValue(modified.encode(), AppConfig.class);
        assertEquals("client-id-from-env", appConfig.clientId);
        assertEquals("db-user-from-env", appConfig.database.username);
    }

    @Test(expected = DecodeException.class)
    public void testMissingAttributeThrowsException() {
        JsonObject modified = configJson.copy();
        modified.remove("clientId");

        Json.decodeValue(modified.encode(), AppConfig.class);
    }
}
