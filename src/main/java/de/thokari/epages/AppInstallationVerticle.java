package de.thokari.epages;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;

public class AppInstallationVerticle extends AbstractVerticle {

    final String clientId = System.getenv("CLIENT_ID") != null
        ? System.getenv("CLIENT_ID")
        : config().getString("client_id");
    final String clientSecret = System.getenv("CLIENT_SECRET") != null
        ? System.getenv("CLIENT_SECRET")
        : config().getString("client_secret");

    final String appUrl = System.getenv("APP_URL") != null ? System.getenv("APP_URL") : config().getString("app_url");
    final String callbackUrlOverride = System.getenv("CALLBACK_URL") != null
        ? System.getenv("CALLBACK_URL")
        : config().getString("callback_url");
    final String callbackUrl = callbackUrlOverride != null ? callbackUrlOverride : (appUrl + "/callback");

    public void start() {

        vertx.eventBus().consumer("app_installation", message -> {
            JsonObject body = (JsonObject) message.body();

            if (!(body.containsKey("code") && body.containsKey("return_url") && body.containsKey("api_url")
                && body.containsKey("access_token_url"))) {
                message.reply(new JsonObject().put("error", "invalid parameters"));
            } else {
                final String code = body.getString("code");
                final String apiUrl = body.getString("api_url");
                final String accessTokenUrl = body.getString("access_token_url");
                final String tokenPath = accessTokenUrl.substring(apiUrl.length());

                OAuth2ClientOptions credentials = new OAuth2ClientOptions().setClientID(clientId)
                    .setClientSecret(clientSecret).setSite(apiUrl).setTokenPath(tokenPath);
                OAuth2Auth oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, credentials);

                oauth2.getToken(new JsonObject().put("code", code).put("redirect_uri", callbackUrl), res -> {
                    if (res.failed()) {
                        message.reply(new JsonObject().put("error", "invalid code"));
                    } else {
                        System.out.println(res.result().toString());
                        message.reply(new JsonObject().put("token", res.result()));
                        // save the token and continue...
                    }
                });
            }

        });

    }

    public void stop() {
    }

}
