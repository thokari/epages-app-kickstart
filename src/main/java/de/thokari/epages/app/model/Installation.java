package de.thokari.epages.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonArray;

import java.sql.Timestamp;
import java.util.Date;
import java.util.stream.Collectors;

public class Installation extends Model {

    public static final String TABLE_NAME = "installations";

    @JsonProperty("api_url")
    public String apiUrl;

    @JsonProperty("access_token")
    public String accessToken;

    @JsonProperty("shop_name")
    public String shopName;

    @JsonProperty("email")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String email;

    @JsonProperty("email_confirmed")
    public Boolean emailConfirmed = false;

    @JsonProperty("created")
    public String created;

    @JsonCreator
    public Installation(@JsonProperty("api_url") String apiUrl, @JsonProperty("access_token") String accessToken,
        @JsonProperty("shop_name") String shopName, @JsonProperty("email") String email,
        @JsonProperty("email_confirmed") Boolean emailConfirmed,
        @JsonProperty("created") String created) {

        this.apiUrl = apiUrl;
        this.accessToken = accessToken;
        this.shopName = shopName;
        this.email = email;
        this.emailConfirmed = emailConfirmed;
        if (created == null) {
            this.created = new Timestamp(new Date().getTime()).toInstant().toString();
        }
    }

    public Installation(String apiUrl, String accessToken, String shopName) {
        this(apiUrl, accessToken, shopName, null, null, null);
    }

    @JsonIgnore
    public String getInsertQuery() {
        return String.format("INSERT INTO %s %s VALUES %s", TABLE_NAME, getColumnNames(), getParameterPart());
    }

    @JsonIgnore
    public String getParameterPart() {
        return this.toJsonObject().stream()
            .filter(it -> it.getValue() != null)
            .map(it -> "?")
            .collect(Collectors.joining(", ", "(", ")"));
    }

    @JsonIgnore
    public String getColumnNames() {
        return this.toJsonObject().stream()
            .filter(it -> it.getValue() != null)
            .map(it -> it.getKey())
            .collect(Collectors.joining(", ", "(", ")"));
    }

    @JsonIgnore
    public JsonArray getInsertQueryParams() {
        return new JsonArray(
            this.toJsonObject().stream()
                .filter(it -> it.getValue() != null)
                .map(it -> it.getValue())
                .collect(Collectors.toList()));
    }

}
