package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Installation;
import de.thokari.epages.app.model.InstallationRequest;
import de.thokari.epages.app.model.Model;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(VertxUnitRunner.class)
public class AppInstallationVerticleTest {

    static final String SHOP_NAME = "Test shop";

    static JsonObject configJson;
    static AppConfig appConfig;
    final Integer apiMockPort = 9999;
    final String apiMockUrl = "http://localhost:" + apiMockPort + "/api";
    final String apiMockTokenUrl = apiMockUrl + "/token";
    final JsonObject installationRequestSource = new JsonObject()
            .put("code", "f32ddSbuff2IGAYvtiwYQiyHyuLJWbey")
            .put("api_url", apiMockUrl)
            .put("access_token_url", apiMockTokenUrl)
            .put("return_url", "http://localhost:8080/epages-app")
            .put("signature", "3AmS3LcnENGi+BEkHY9Bk0D0pFUnmsudemQqjz9lybo=");
    final InstallationRequest installationEvent = Model.fromJsonObject(installationRequestSource,
            InstallationRequest.class);
    final JsonObject tokenResponse = new JsonObject()
            .put("access_token", "4HZ9hriF6J3GOnd10JbFzdVehycOvAZf");
    final Vertx vertx = Vertx.vertx();
    final DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(configJson);
    final Promise<HttpServer> apiMockStarted = Promise.promise();
    final Promise<Void> databasePrepared = Promise.promise();
    AsyncSQLClient dbClient;
    HttpServer apiMock;
    MessageConsumer<JsonObject> apiClientMock;

    @BeforeClass
    public static void readConfig() throws IOException {
        configJson = new JsonObject(new String(Files.readAllBytes(Paths.get("config.test.json"))));
        appConfig = Model.fromJsonObject(configJson, AppConfig.class);
    }

    @Before
    public void initDatabase() {
        dbClient = PostgreSQLClient.createNonShared(vertx, appConfig.database.toJsonObject());
        dbClient.getConnection(connected -> {
            String sql = String.format("DELETE FROM %s WHERE api_url = '%s'", "installations", apiMockUrl);
            connected.result().execute(sql, databasePrepared);
        });
    }

    @Before
    public void startMocks() {
        apiMock = vertx.createHttpServer().requestHandler(request -> {
            if (request.absoluteURI().equals(apiMockTokenUrl)) {
                request.response().headers().add("Content-Type", "application/json");
                request.response().end(tokenResponse.toString());
            } else {
                request.response().setStatusCode(404).end();
            }
        }).listen(apiMockPort, apiMockStarted);

        apiClientMock = vertx.eventBus().consumer(EpagesApiClientVerticle.EVENT_BUS_ADDRESS, message -> {
            if ("get-shop-info".equals(message.body().getString("action"))) {
                message.reply(new JsonObject().put("name", SHOP_NAME));
            } else {
                message.fail(500, String.format("API request to '%s' failed", apiMockUrl));
            }
        });
    }

    @After
    public void stopApiMockAndDatabase(TestContext context) {
        Async async = context.async();
        Promise<Void> apiMockClosed = Promise.promise();
        Promise<Void> dbClientClosed = Promise.promise();
        Promise<Void> apiClientMockClosed = Promise.promise();
        apiMock.close(apiMockClosed);
        dbClient.close(dbClientClosed);
        apiClientMock.unregister(apiClientMockClosed);
        CompositeFuture.all(
                apiMockClosed.future(),
                dbClientClosed.future(),
                apiClientMockClosed.future()
        ).setHandler(closed -> async.complete());
        async.awaitSuccess(1000);
    }

    @Test
    public void testAppInstallationOauthDance(TestContext context) {
        Async async = context.async();

        // GIVEN

        CompositeFuture.all(apiMockStarted.future(), databasePrepared.future()).setHandler(started -> {
            if (started.failed()) {
                started.cause().printStackTrace();
                context.fail();
                async.complete();
            }

            vertx.deployVerticle(AppInstallationVerticle.class.getName(), deploymentOpts, deployed -> {
                if (deployed.failed()) {
                    deployed.cause().printStackTrace();
                    context.fail();
                    async.complete();
                }

                // WHEN

                vertx.eventBus().<JsonObject>request(
                        AppInstallationVerticle.EVENT_BUS_ADDRESS,
                        installationEvent.toJsonObject(),
                        response -> {

                            // THEN
                            context.assertTrue(response.succeeded(),
                                    response.cause() != null ? response.cause().getMessage() : "<no error message>");
                            context.assertEquals(null, response.result().body());

                            dbClient.getConnection(connected -> {
                                if (connected.failed()) {
                                    connected.cause().printStackTrace();
                                    context.fail();
                                    async.complete();
                                }
                                connected.result().query(
                                        String.format("SELECT * FROM installations WHERE api_url = '%s'",
                                                installationEvent.apiUrl),
                                        result -> {
                                            if (result.failed()) {
                                                result.cause().printStackTrace();
                                                context.fail();
                                            }
                                            context.assertEquals(1, result.result().getNumRows());
                                            Installation installation = Model.fromJsonObject(result.result().getRows().get(0), Installation.class);
                                            context.assertEquals(tokenResponse.getString("access_token"), installation.accessToken);
                                            context.assertEquals(SHOP_NAME, installation.shopName);
                                            async.complete();
                                        });
                            });
                        });
            });
        });
        async.awaitSuccess(2000);
    }

    @Test
    public void testDatabaseConnectionError(TestContext context) {
        Async async = context.async();

        // GIVEN

        CompositeFuture.all(apiMockStarted.future(), databasePrepared.future()).setHandler(started -> {
            if (started.failed()) {
                started.cause().printStackTrace();
                context.fail();
                async.complete();
            }

            JsonObject dbConfig = configJson.getJsonObject("database").put("database", "nonexistent");
            DeploymentOptions wrongDbConfig = new DeploymentOptions()
                    .setConfig(configJson.put("database", dbConfig));

            vertx.deployVerticle(AppInstallationVerticle.class.getName(), wrongDbConfig, deployed -> {
                if (deployed.failed()) {
                    deployed.cause().printStackTrace();
                    context.fail();
                    async.complete();
                }

                // WHEN

                vertx.eventBus().<JsonObject>request(
                        AppInstallationVerticle.EVENT_BUS_ADDRESS,
                        installationEvent.toJsonObject(),
                        response -> {

                            // THEN
                            context.assertTrue(response.failed());
                            String expectedMessage = "could not create installation for event";
                            String actualMessage = response.cause().getMessage().substring(0, expectedMessage.length());
                            context.assertEquals(expectedMessage, actualMessage);
                            async.complete();
                        });
            });
        });
        async.awaitSuccess(2000);
    }

    @Test
    public void testTokenError(TestContext context) {
        Async async = context.async();

        // GIVEN

        Promise<Void> apiMockClosed = Promise.promise();
        apiMockStarted.future().setHandler(started -> apiMock.close(apiMockClosed));

        CompositeFuture.all(apiMockClosed.future(), databasePrepared.future()).setHandler(started -> {
            if (started.failed()) {
                started.cause().printStackTrace();
                context.fail();
                async.complete();
            }

            vertx.deployVerticle(AppInstallationVerticle.class.getName(), deploymentOpts, deployed -> {
                if (deployed.failed()) {
                    deployed.cause().printStackTrace();
                    context.fail();
                    async.complete();
                }

                // WHEN

                vertx.eventBus().<JsonObject>request(
                        AppInstallationVerticle.EVENT_BUS_ADDRESS,
                        installationEvent.toJsonObject(),
                        response -> {

                            // THEN
                            context.assertTrue(response.failed());
                            String expectedMessage = "could not get token for event";
                            String actualMessage = response.cause().getMessage().substring(0, expectedMessage.length());
                            context.assertEquals(expectedMessage, actualMessage);
                            async.complete();
                        });
            });
        });
        async.awaitSuccess(2000);
    }

    @Test
    public void testShopInfoError(TestContext context) {
        Async async = context.async();

        // GIVEN

        apiClientMock.unregister();
        apiClientMock = vertx.eventBus().consumer(EpagesApiClientVerticle.EVENT_BUS_ADDRESS, message -> {
            if ("get-shop-info".equals(message.body().getString("action"))) {
                message.fail(500, String.format("API request to '%s' failed", apiMockUrl));
            }
        });

        databasePrepared.future().setHandler(started -> {
            if (started.failed()) {
                started.cause().printStackTrace();
                context.fail();
                async.complete();
            }

            vertx.deployVerticle(AppInstallationVerticle.class.getName(), deploymentOpts, deployed -> {
                if (deployed.failed()) {
                    deployed.cause().printStackTrace();
                    context.fail();
                    async.complete();
                }

                // WHEN

                vertx.eventBus().<JsonObject>request(
                        AppInstallationVerticle.EVENT_BUS_ADDRESS,
                        installationEvent.toJsonObject(),
                        response -> {

                            // THEN
                            context.assertTrue(response.failed());
                            String expectedMessage = String.format("API request to '%s' failed", apiMockUrl);
                            String actualMessage = response.cause().getMessage().substring(0, expectedMessage.length());
                            context.assertEquals(expectedMessage, actualMessage);
                            async.complete();
                        });
            });
        });
        async.awaitSuccess(2000);
    }
}
