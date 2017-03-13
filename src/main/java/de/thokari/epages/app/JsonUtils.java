package de.thokari.epages.app;

import io.vertx.core.json.JsonObject;

public class JsonUtils {

    public static JsonObject serverErrorReply(int code) {
        return errorReply().put("code", code);
    }
    
    public static JsonObject serverErrorReply() {
        return errorReply().put("code", 500);
    }
    
    public static JsonObject clientErrorReply() {
        return errorReply().put("code", 400);
    }

    public static JsonObject errorReply() {
        return new JsonObject().put("status", "error");
    }

    public static JsonObject okReply() {
        return new JsonObject().put("status", "ok");
    }
}
