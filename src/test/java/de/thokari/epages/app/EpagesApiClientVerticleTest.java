package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Model;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(VertxUnitRunner.class)
public class EpagesApiClientVerticleTest {

    static JsonObject configJson;
    static AppConfig appConfig;
    final Integer apiMockPort = 9999;
    final String apiMockUrl = "http://localhost:" + apiMockPort + "/api";
    final String token = "Q9T6g8td0lHzQbIg9CwgRNCrU1SfCko4";
    final JsonObject apiCall = new JsonObject()
            .put("action", "shop-info")
            .put("token", token)
            .put("apiUrl", apiMockUrl);
    final Vertx vertx = Vertx.vertx();
    // .put("apiUrl",
    // "https://devshop.epages.com/rs/shops/epagesdevD20161020T212339R164");
    final DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(configJson);
    final Future<HttpServer> apiMockStarted = Future.future();
    HttpServer apiMock;

    @BeforeClass
    public static void readConfig() throws IOException {
        configJson = new JsonObject(new String(Files.readAllBytes(Paths.get("config.test.json"))));
        appConfig = Model.fromJsonObject(configJson, AppConfig.class);
    }

    @Before
    public void startApiMock() {
        apiMock = vertx.createHttpServer().requestHandler(request -> {
            if (apiMockUrl.equals(request.absoluteURI())) {
                request.response()
                        .setStatusCode(200)
                        .end(new JsonObject().put("name", "Milestones").encodePrettily());
            } else {
                request.response().setStatusCode(404).end();
            }
        }).listen(apiMockPort, apiMockStarted);
    }

    @Test
    public void testApiCallFailed(TestContext context) {
        Async async = context.async();

        apiMockStarted.setHandler(started -> {
            if (apiMockStarted.failed()) {
                apiMockStarted.cause().printStackTrace();
                context.fail();
                async.complete();
            }

            apiMock.close(closed -> {

                vertx.deployVerticle(EpagesApiClientVerticle.class.getName(), deploymentOpts, deployed -> {
                    if (deployed.failed()) {
                        deployed.cause().printStackTrace();
                        context.fail();
                        async.complete();
                    }

                    vertx.eventBus().<JsonObject>send(
                            EpagesApiClientVerticle.EVENT_BUS_ADDRESS, apiCall, response -> {
                                context.assertTrue(response.failed());
                                context.assertTrue(response.cause().getMessage().startsWith(String.format("API request to '%s' failed because of 'Connection refused", apiMockUrl)));
                                context.assertEquals(500, ((ReplyException) response.cause()).failureCode());
                                async.complete();
                            });
                });
            });
        });
        async.awaitSuccess(2000);
    }

    @Test
    public void testShopInfoCall(TestContext context) {
        Async async = context.async();

        apiMockStarted.setHandler(started -> {
            if (apiMockStarted.failed()) {
                apiMockStarted.cause().printStackTrace();
                context.fail();
                async.complete();
            }

            vertx.deployVerticle(EpagesApiClientVerticle.class.getName(), deploymentOpts, deployed -> {
                if (deployed.failed()) {
                    deployed.cause().printStackTrace();
                    context.fail();
                    async.complete();
                }

                vertx.eventBus().<JsonObject>send(
                        EpagesApiClientVerticle.EVENT_BUS_ADDRESS, apiCall, response -> {
                            context.assertTrue(response.succeeded());
                            JsonObject body = response.result().body();
                            context.assertEquals("Milestones", body.getString("name"));
                            async.complete();
                        });
            });
        });
        async.awaitSuccess(2000);
    }
}
