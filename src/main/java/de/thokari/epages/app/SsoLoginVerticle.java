package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Installation;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;

public class SsoLoginVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "sso_login";

    private static final Logger LOG = LoggerFactory.getLogger(SsoLoginVerticle.class);
    private AsyncSQLClient dbClient;

    public void start() {

        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);
        dbClient = PostgreSQLClient.createShared(vertx, appConfig.database.toJsonObject());

        vertx.eventBus().<String>consumer(EVENT_BUS_ADDRESS).handler(handleLoginEvent());
    }

    private Handler<Message<String>> handleLoginEvent() {
        return message -> {

            String shopPublicId = message.body();
            LOG.info(String.format("received login request for shop '%s'", shopPublicId));

            getInstallationFromDb(shopPublicId).setHandler(result -> {
                if (result.succeeded()) {
                    Installation installation = result.result();

                    JsonObject apiRequestData = new JsonObject() //
                        .put("action", "singlesignon") //
                        .put("apiUrl", installation.apiUrl) //
                        .put("token", installation.accessToken);

                    vertx.eventBus().<JsonObject>send(EpagesApiClientVerticle.EVENT_BUS_ADDRESS, apiRequestData,
                        reply -> {
                            if (reply.succeeded()) {
                                // TODO here we are stuck for obvious reasons
                                System.out.println("Request cannot succeed...");
                                message.reply(reply.result().body().getString("ssourl"));
                            } else {
                                ReplyException error = (ReplyException) reply.cause();
                                message.fail(error.failureCode(), error.getMessage());
                            }
                        });
                } else {
                    String errorMsg = result.cause().getMessage();
                    LOG.error(errorMsg);
                    message.fail(500, errorMsg);
                }
            });
        };
    };

    private Future<Installation> getInstallationFromDb(String shopPublicId) {
        Future<Installation> future = Future.future();
        dbClient.getConnection(connected -> {
            if (connected.failed()) {
                future.fail(connected.cause().getMessage());
            } else {
                SQLConnection connection = connected.result();
                String sql = String.format("SELECT * FROM installations WHERE api_url LIKE '%%%s%%'", shopPublicId);

                connection.query(sql, queryResult -> {
                    if (queryResult.failed()) {
                        future.fail(queryResult.cause().getMessage());
                    } else {
                        try {
                            JsonObject result = queryResult.result().getRows().get(0);
                            Installation installation = Installation.fromJsonObject(result, Installation.class);
                            future.complete(installation);
                            LOG.debug(
                                String.format("retrieved installation for shop '%s' from database", shopPublicId));
                        } catch (Exception e) {
                            future.fail(String.format("no installation found for shop '%s'", shopPublicId));
                        }
                    }
                    connection.close();
                });
            }
        });
        return future;
    }

}
