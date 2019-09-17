package de.thokari.epages.app.model;

abstract class Config extends Model {

    static <T> T overrideFromEnv(final String envVar, final T value, final Class<T> clazz) {
        String fromEnv = System.getenv(envVar);
        if (fromEnv != null) {
            return clazz.cast(fromEnv);
        }
        return value;
    }

    static String overrideFromEnv(final String envVar, final String value) {
        return overrideFromEnv(envVar, value, String.class);
    }

}
