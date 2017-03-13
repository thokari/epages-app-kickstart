package de.thokari.epages.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatabaseConfig extends Config {

    public String host;
    
    public Integer port;
    
    public String username;
    
    public String password;
    
    public String database;
    
    public String charset;
    
    public Integer maxPoolSize;
    
    public Integer queryTimeout;

    @JsonCreator
    public DatabaseConfig(
        @JsonProperty("host") String host,
        @JsonProperty("port") Integer port,
        @JsonProperty("maxPoolSize") Integer maxPoolSize,
        @JsonProperty("username") String username,
        @JsonProperty("password") String password,
        @JsonProperty("database") String database,
        @JsonProperty("charset") String charset,
        @JsonProperty("queryTimeout") Integer queryTimeout) {

        this.host = validate("host", overrideFromEnv("DB_HOST", host));
        this.port = validate("port", overrideFromEnv("DB_PORT", port, Integer.class));
        this.username = validate("username", overrideFromEnv("DB_USERNAME", username));
        this.password = validate("password", overrideFromEnv("DB_PASSWORD", password));
        this.database = validate("database", overrideFromEnv("DB_DATABASE", database));
        this.charset = validate("charset", overrideFromEnv("DB_CHARSET", charset));
        this.maxPoolSize = validate("maxPoolSize", overrideFromEnv("DB_MAX_POOL_SIZE", maxPoolSize, Integer.class));
        this.queryTimeout = validate("queryTimeout", overrideFromEnv("DB_QUERY_TIMEOUT", queryTimeout, Integer.class));
    }
}
