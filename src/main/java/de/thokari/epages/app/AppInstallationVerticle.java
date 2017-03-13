package de.thokari.epages.app;

import static de.thokari.epages.app.JsonUtils.okReply;
import static de.thokari.epages.app.JsonUtils.serverErrorReply;

import java.net.MalformedURLException;
import java.util.Date;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.InstallationRequest;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.sql.SQLConnection;

public class AppInstallationVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "app_installation";

    public void start() throws MalformedURLException {

        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);

        vertx.eventBus()
            .<JsonObject>consumer(EVENT_BUS_ADDRESS,
                handleInstallationRequest(
                    appConfig.clientId,
                    appConfig.clientSecret));
    }

    private Handler<Message<JsonObject>> handleInstallationRequest(String clientId, String clientSecret) {
        return message -> {
            InstallationRequest event = Model.fromJsonObject(message.body(), InstallationRequest.class);

            OAuth2ClientOptions credentials = new OAuth2ClientOptions()
                .setClientID(clientId).setClientSecret(clientSecret)
                .setSite(event.apiUrl).setTokenPath(event.tokenPath);

            requestOAuth2Token(credentials, event.code, event.returnUrl)
                .setHandler(tokenResult -> {
                    performInstallation(tokenResult, message, event);
                });
        };
    }

    private Future<AccessToken> requestOAuth2Token(
        OAuth2ClientOptions credentials, String code, String returnUrl) {

        Future<AccessToken> future = Future.future();
        OAuth2Auth oAuth2 = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, credentials);
        
        JsonObject tokenParameters = new JsonObject().put("code", code).put("redirect_uri", returnUrl);
        
        oAuth2.getToken(tokenParameters, future.completer());

        return future;
    }

    private void performInstallation(
        AsyncResult<AccessToken> tokenResponse, Message<JsonObject> message, InstallationRequest event) {

        if (tokenResponse.failed()) {
            tokenResponse.cause().printStackTrace();
            message.reply(serverErrorReply().put("message", tokenResponse.cause().getMessage()));
        } else {
            String token = tokenResponse.result().principal().getString("access_token");
            Database.withConnection().setHandler(saveToken(token, message));
        }
    }

    private Handler<AsyncResult<SQLConnection>> saveToken(String token, Message<JsonObject> message) {
        return connected -> {
            if (connected.failed()) {
                connected.cause().printStackTrace();
                message.reply(serverErrorReply().put("message", connected.cause().getMessage()));
            } else {
                SQLConnection connection = connected.result();

                String sql = "INSERT INTO installations (token, created) VALUES (?, CAST(? AS DATE))";

                JsonArray params = new JsonArray().add(token).add(new Date().toString());

                connection.queryWithParams(sql, params, queryResult -> {
                    if (queryResult.failed()) {
                        queryResult.cause().printStackTrace();
                        message.reply(serverErrorReply().put("message", queryResult.cause().getMessage()));
                    } else {
                        message.reply(okReply());
                    }
                });
            }
        };
    }
}
