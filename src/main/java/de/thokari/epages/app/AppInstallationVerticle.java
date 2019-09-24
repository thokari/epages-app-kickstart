package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Installation;
import de.thokari.epages.app.model.InstallationRequest;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.sql.SQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.format;

public class AppInstallationVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "app_installation";
    private static final Logger LOG = LoggerFactory.getLogger(AppInstallationVerticle.class);
    private AsyncSQLClient dbClient;

    public void start() {

        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);
        dbClient = PostgreSQLClient.createShared(vertx, appConfig.database.toJsonObject());

        vertx.eventBus().<JsonObject>consumer(EVENT_BUS_ADDRESS, message ->
                replyToInstallationRequest(message, appConfig.clientId, appConfig.clientSecret)
        );
    }

    private void replyToInstallationRequest(Message<JsonObject> message, String clientId, String clientSecret) {
        InstallationRequest request = Model.fromJsonObject(message.body(), InstallationRequest.class);
        if (!request.hasValidSignature(clientSecret)) {
            String errorMsg = format("invalid signature on installation request '{}'", request.toString());
            LOG.error(errorMsg);
            message.fail(400, errorMsg);
        } else {
            LOG.debug("received installation event '{}'", request.toString());

            requestOAuth2Token(clientId, clientSecret, request).setHandler(accessTokenResult -> {
                if (accessTokenResult.failed()) {
                    LOG.trace("OAuth token handler failed");
                    String errorMsg = format("could not get token for event '{}' because of '{}'",
                            request.toString(), accessTokenResult.cause().getMessage());
                    LOG.error(errorMsg);
                    message.fail(500, errorMsg);
                } else {
                    LOG.trace("OAuth token handler successful");
                    AccessToken accessToken = accessTokenResult.result();
                    String tokenValue = accessToken.principal().getString("access_token");
                    LOG.debug("obtained access token '{}' for API URL '{}'", tokenValue, request.apiUrl);

                    getShopInfo(tokenValue, request).setHandler(getShopInfoResult -> {
                        if (getShopInfoResult.failed()) {
                            LOG.trace("shop info handler failed");
                            message.fail(500, getShopInfoResult.cause().getMessage());
                        } else {
                            LOG.trace("shop info handler successful");
                            JsonObject shopInfo = getShopInfoResult.result();

                            createInstallation(tokenValue, shopInfo, request).setHandler(installationResult -> {
                                if (installationResult.failed()) {
                                    LOG.trace("save installation handler failed");
                                    String errorMsg = String.format(
                                            "could not create installation for event '%s' because of '%s'",
                                            request.toString(), installationResult.cause().getMessage());
                                    LOG.error(errorMsg);
                                    message.fail(500, errorMsg);
                                } else {
                                    LOG.trace("save installation handler successful");
                                    message.reply(installationResult.result());
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private Future<AccessToken> requestOAuth2Token(String clientId, String clientSecret, InstallationRequest request) {
        Promise<AccessToken> promise = Promise.promise();
        OAuth2ClientOptions options = new OAuth2ClientOptions()
                .setFlow(OAuth2FlowType.AUTH_CODE)
                .setClientID(clientId).setClientSecret(clientSecret)
                .setSite(request.apiUrl).setTokenPath(request.tokenPath);
        OAuth2Auth oAuth2 = OAuth2Auth.create(vertx, options);
        JsonObject tokenParameters = new JsonObject()
                .put("code", request.code)
                .put("redirect_uri", request.returnUrl);
        oAuth2.getToken(tokenParameters, promise);
        return promise.future();
    }

    private Future<JsonObject> createInstallation(String accessToken, JsonObject shopInfo, InstallationRequest request) {
        Promise<JsonObject> promise = Promise.promise();
        Installation installation = new Installation(request.apiUrl, accessToken, shopInfo.getString("name"));
        saveInstallation(installation).setHandler(promise);
        return promise.future();
    }

    private Future<JsonObject> getShopInfo(String accessToken, InstallationRequest request) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject apiRequest = new JsonObject()
                .put("action", "get-shop-info")
                .put("apiUrl", request.apiUrl)
                .put("token", accessToken);
        vertx.eventBus().<JsonObject>request(EpagesApiClientVerticle.EVENT_BUS_ADDRESS, apiRequest, result -> {
            if (result.succeeded()) {
                promise.complete(result.result().body());
            } else {
                promise.fail(result.cause().getMessage());
            }
        });
        return promise.future();
    }

    private Future<JsonObject> saveInstallation(Installation installation) {
        Promise<JsonObject> promise = Promise.promise();
        dbClient.getConnection(connected -> {
            if (connected.failed()) {
                promise.fail(connected.cause().getMessage());
            } else {
                SQLConnection connection = connected.result();
                String sql = installation.getInsertQuery();
                JsonArray params = installation.getInsertQueryParams();

                LOG.debug("executing query '{}' with parameters '{}'", sql, params.encode());

                connection.queryWithParams(sql, params, queryResult -> {
                    if (queryResult.failed()) {
                        promise.fail(queryResult.cause().getMessage());
                    } else {
                        LOG.info("installation '{}' saved", installation.toJsonObject().toString());
                        promise.complete();
                    }
                    connection.close();
                });
            }
        });
        return promise.future();
    }
}
