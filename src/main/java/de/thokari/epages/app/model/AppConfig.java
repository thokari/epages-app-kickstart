package de.thokari.epages.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppConfig extends Config {

    public String clientId;

    public String clientSecret;

    public Boolean appUseSsl;

    public Boolean appCertSelfSigned;

    public String appHostname;

    public String appDomain;

    public Integer appPort;

    public String appStaticPath;

    public String appApiPath;

    public String callbackPath;

    public DatabaseConfig database;

    @JsonCreator
    public AppConfig(
            @JsonProperty("clientId") String clientId,
            @JsonProperty("clientSecret") String clientSecret,
            @JsonProperty("appUseSsl") Boolean appUseSsl,
            @JsonProperty("appCertSelfSigned") Boolean appCertSelfSigned,
            @JsonProperty("appHostname") String appHostname,
            @JsonProperty("appDomain") String appDomain,
            @JsonProperty("appPort") Integer appPort,
            @JsonProperty("appStaticPath") String appStaticPath,
            @JsonProperty("appApiPath") String appApiPath,
            @JsonProperty("callbackPath") String callbackPath,
            @JsonProperty("database") DatabaseConfig database) {

        this.clientId = validate("clientId", overrideFromEnv("CLIENT_ID", clientId));
        this.clientSecret = validate("clientSecret", overrideFromEnv("CLIENT_SECRET", clientSecret));
        this.appUseSsl = validate("appUseSsl", overrideFromEnv("APP_USE_SSL", Boolean.valueOf(appUseSsl), Boolean.class));
        this.appCertSelfSigned = validate("appCertSelfSigned", overrideFromEnv("APP_CERT_SELF_SIGNED", Boolean.valueOf(appCertSelfSigned), Boolean.class));
        this.appHostname = validate("appHostname", overrideFromEnv("APP_HOSTNAME", appHostname));
        this.appDomain = validate("appDomain", overrideFromEnv("APP_DOMAIN", appDomain));
        this.appPort = validate("appPort", overrideFromEnv("APP_PORT", appPort, Integer.class));
        this.appStaticPath = validate("appStaticPath", overrideFromEnv("APP_STATIC_PATH", appStaticPath));
        this.appApiPath = validate("appApiPath", overrideFromEnv("APP_API_PATH", appApiPath));
        this.callbackPath = validate("callbackPath", overrideFromEnv("APP_CALLBACK_PATH", callbackPath));
        this.database = database;
    }

    @JsonIgnore
    public String getFqdn() {
        return String.format("%s.%s", appHostname, appDomain);
    }
}