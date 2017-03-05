package de.thokari.epages;

import io.vertx.core.json.JsonObject;

public class JsonUtils {

    public static String getString(final JsonObject config, final String configKey, final String envVar) {
        return System.getenv(envVar) != null ? System.getenv(envVar) : config.getString(configKey);
    }
    
    public static Integer getInteger(final JsonObject config, final String configKey, final String envVar) {
        return System.getenv(envVar) != null ? Integer.valueOf(System.getenv(envVar)) : config.getInteger(configKey);
    }

    public static JsonObject errorReply() {
        return new JsonObject().put("status", "error");
    }

    public static JsonObject okReply() {
        return new JsonObject().put("status", "ok");
    }
}
