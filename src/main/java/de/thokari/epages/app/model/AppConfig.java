package de.thokari.epages.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppConfig extends Config {

    public String clientId;

    public String clientSecret;

    public String appProtocol;

    public String appHostname;

    public Integer appPort;

    public String appStaticPath;

    public String appApiPath;

    public String callbackPath;

    public DatabaseConfig database;

    @JsonCreator
    public AppConfig(
            @JsonProperty("clientId") String clientId,
            @JsonProperty("clientSecret") String clientSecret,
            @JsonProperty("appProtocol") String appProtocol,
            @JsonProperty("appHostname") String appHostname,
            @JsonProperty("appPort") Integer appPort,
            @JsonProperty("appStaticPath") String appStaticPath,
            @JsonProperty("appApiPath") String appApiPath,
            @JsonProperty("callbackPath") String callbackPath,
            @JsonProperty("database") DatabaseConfig database) {

        this.clientId = validate("clientId", overrideFromEnv("CLIENT_ID", clientId));
        this.clientSecret = validate("clientSecret", overrideFromEnv("CLIENT_SECRET", clientSecret));
        this.appProtocol = validate("appProtocol", overrideFromEnv("APP_PROTOCOL", appProtocol));
        this.appHostname = validate("appHostname", overrideFromEnv("APP_HOSTNAME", appHostname));
        this.appPort = validate("appPort", overrideFromEnv("APP_PORT", appPort, Integer.class));
        this.appStaticPath = validate("appStaticPath", overrideFromEnv("APP_STATIC_PATH", appStaticPath));
        this.appApiPath = validate("appApiPath", overrideFromEnv("APP_API_PATH", appApiPath));
        this.callbackPath = validate("callbackPath", overrideFromEnv("APP_CALLBACK_PATH", callbackPath));
        this.database = database;
    }
}
