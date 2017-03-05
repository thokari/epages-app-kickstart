package de.thokari.epages;

import io.vertx.core.json.JsonObject;

public class JsonUtils {

    public static JsonObject errorReply() {
        return new JsonObject().put("status", "error");
    }

    public static JsonObject okReply() {
        return new JsonObject().put("status", "ok");
    }
}
