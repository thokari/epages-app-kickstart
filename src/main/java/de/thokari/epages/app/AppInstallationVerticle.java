package de.thokari.epages.app;

import static de.thokari.epages.app.JsonUtils.okReply;
import static de.thokari.epages.app.JsonUtils.serverErrorReply;

import java.net.MalformedURLException;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Installation;
import de.thokari.epages.app.model.InstallationRequest;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.sql.SQLConnection;

public class AppInstallationVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "app_installation";
    private static final Logger LOG = LoggerFactory.getLogger(AppInstallationVerticle.class);
    private AsyncSQLClient dbClient;

    public void start() throws MalformedURLException {

        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);
        dbClient = PostgreSQLClient.createShared(vertx, appConfig.database.toJsonObject());

        vertx.eventBus()
            .<JsonObject>consumer(EVENT_BUS_ADDRESS,
                handleInstallationRequest(appConfig.clientId, appConfig.clientSecret));
    }

    private Handler<Message<JsonObject>> handleInstallationRequest(String clientId, String clientSecret) {
        return message -> {
            InstallationRequest event = Model.fromJsonObject(message.body(), InstallationRequest.class);

            LOG.info("received installation event " + event.toString());

            OAuth2ClientOptions credentials = new OAuth2ClientOptions()
                .setClientID(clientId).setClientSecret(clientSecret)
                .setSite(event.apiUrl).setTokenPath(event.tokenPath);

            requestOAuth2Token(credentials, event.code, event.returnUrl)

                .compose(tokenResult -> {
                    return createInstallation(tokenResult, event).otherwise(installationError -> {
                        String errorMsg = String.format("could not create installation for event %s", event.toString());
                        LOG.error(errorMsg);
                        return serverErrorReply(errorMsg, installationError.getMessage());
                    });
                })

                .otherwise(tokenError -> {
                    String errorMsg = String.format("could not get token for event %s", event.toString());
                    LOG.error(errorMsg);
                    return serverErrorReply(errorMsg, tokenError.getMessage());
                })
                
                .setHandler(
                    installationResult -> message.reply(installationResult.result()));
        };
    };

    private Future<AccessToken> requestOAuth2Token(OAuth2ClientOptions credentials, String code, String returnUrl) {
        Future<AccessToken> future = Future.future();

        OAuth2Auth oAuth2 = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, credentials);
        JsonObject tokenParameters = new JsonObject().put("code", code).put("redirect_uri", returnUrl);

        oAuth2.getToken(tokenParameters, future);
        return future;
    }

    private Future<JsonObject> createInstallation(AccessToken token, InstallationRequest event) {
        Future<JsonObject> future = Future.future();

        String accessToken = token.principal().getString("access_token");
        // TODO shop name etc.
        Installation installation = new Installation("Milestones", event.apiUrl, accessToken);
        
        saveInstallation(installation).setHandler(future);
        return future;
    }

    private Future<JsonObject> saveInstallation(Installation installation) {
        Future<JsonObject> future = Future.future();
        dbClient.getConnection(connected -> {
            if (connected.failed()) {
                future.fail(connected.cause().getMessage());
            } else {
                SQLConnection connection = connected.result();
                String sql = installation.getInsertQuery();
                JsonArray params = installation.getInsertQueryParams();

                LOG.debug("executing query " + sql + " with parameters " + params.encode());

                connection.queryWithParams(sql, params, queryResult -> {
                    if (queryResult.failed()) {
                        future.fail(queryResult.cause().getMessage());
                    } else {
                        LOG.info(String.format("installation %s saved", installation.toJsonObject().toString()));
                        future.complete(okReply());
                    }
                    connection.close();
                });
            }
        });
        return future;
    }
}
