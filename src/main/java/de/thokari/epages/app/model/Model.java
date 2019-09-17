package de.thokari.epages.app.model;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public abstract class Model {

    static <T> T validate(final String key, final T value) {
        if (null == value) {
            throw new IllegalArgumentException(key + " must not be null");
        } else {
            return value;
        }
    }

    public static <T extends Model> T fromJsonObject(JsonObject source, Class<T> clazz) {
        return Json.decodeValue(source.encode(), clazz);
    }

    public JsonObject toJsonObject() {
        return new JsonObject(Json.encode(this));
    }

    public String toString() {
        return Json.encode(this);
    }
}
