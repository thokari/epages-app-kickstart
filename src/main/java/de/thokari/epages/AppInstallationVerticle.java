package de.thokari.epages;

import static de.thokari.epages.JsonUtils.errorReply;
import static de.thokari.epages.JsonUtils.okReply;

import java.net.MalformedURLException;
import java.net.URL;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;

public class AppInstallationVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "app_installation";

    public void start() throws MalformedURLException {

        final String clientId = config().getString("client_id");
        final String clientSecret = config().getString("client_secret");
        final String appProtocol = config().getString("app_protocol");
        final String appHostname = config().getString("app_hostname");
        final Integer appPort = config().getInteger("app_port");
        final String callbackPath = config().getString("callback_path");
        final String callbackUrl = new URL(appProtocol, appHostname, appPort, callbackPath).toString();

        vertx.eventBus().consumer(EVENT_BUS_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();

            final String code = body.getString("code");
            final String apiUrl = body.getString("api_url");
            final String accessTokenUrl = body.getString("access_token_url");
            final String tokenPath = accessTokenUrl.substring(apiUrl.length());

            if (!(code != null && apiUrl != null && accessTokenUrl != null && tokenPath != null)) {
                message.reply(errorReply().put("message", "invalid parameters"));
            } else {

                OAuth2ClientOptions credentials = new OAuth2ClientOptions()
                    .setClientID(clientId).setClientSecret(clientSecret)
                    .setSite(apiUrl).setTokenPath(tokenPath);
                OAuth2Auth oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, credentials);

                oauth2.getToken(new JsonObject().put("code", code).put("redirect_uri", callbackUrl), res -> {
                    if (res.failed()) {
                        res.cause().printStackTrace();
                        message.reply(new JsonObject().put("status", "error").put("message", res.cause().getMessage()));
                    } else {
                        String token = res.result().principal().getString("access_token");
                        System.out.println(token);
                        // save the token and continue...
                        message.reply(okReply());
                    }
                });
            }

        });

    }

    public void stop() {
    }

}
