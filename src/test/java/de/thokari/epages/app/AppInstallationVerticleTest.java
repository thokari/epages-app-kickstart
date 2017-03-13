package de.thokari.epages.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.thokari.epages.app.AppInstallationVerticle;
import de.thokari.epages.app.Database;
import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.InstallationRequest;
import de.thokari.epages.app.model.Model;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AppInstallationVerticleTest {

    final Integer apiMockPort = 9999;
    final String apiMockUrl = "http://localhost:" + apiMockPort + "/api";
    final String apiMockTokenUrl = apiMockUrl + "/token";

    final JsonObject installationEventSource = new JsonObject()
        .put("code", "f32ddSbuff2IGAYvtiwYQiyHyuLJWbey")
        .put("api_url", apiMockUrl)
        .put("access_token_url", apiMockTokenUrl)
        .put("return_url", "http://localhost:8080/epages-app");
    
    final InstallationRequest installationEvent = Model.fromJsonObject(installationEventSource,
        InstallationRequest.class);

    final JsonObject tokenResponse = new JsonObject()
        .put("access_token", "4HZ9hriF6J3GOnd10JbFzdVehycOvAZf");

    static JsonObject configJson;
    static AppConfig appConfig;

    final Vertx vertx = Vertx.vertx();
    final DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(configJson);

    final Future<HttpServer> apiMockStarted = Future.future();

    @BeforeClass
    public static void readConfig() throws IOException {
        configJson = new JsonObject(new String(Files.readAllBytes(Paths.get("config.test.json"))));
        appConfig = Model.fromJsonObject(configJson, AppConfig.class);
    }

    @Before
    public void initDatabase() {
        Database.init(vertx, appConfig.database);
    }

    @Before
    public void startApiMock() {
        vertx.createHttpServer().requestHandler(request -> {

            if (request.absoluteURI().equals(apiMockTokenUrl)) {
                request.response().headers().add("Content-Type", "application/json");
                request.response().end(tokenResponse.toString());
            } else {
                request.response().setStatusCode(404).end();
            }
        }).listen(apiMockPort, apiMockStarted.completer());
    }

    @Test
    public void testAppInstallationOauthDance(TestContext context) {
        Async async = context.async();

        apiMockStarted.setHandler(started -> {
            if (apiMockStarted.failed()) {
                apiMockStarted.cause().printStackTrace();
                context.fail();
                async.complete();
            }
            
            vertx.deployVerticle(AppInstallationVerticle.class.getName(), deploymentOpts, deployed -> {
                if (deployed.failed()) {
                    deployed.cause().printStackTrace();
                    context.fail();
                    async.complete();
                }
                
                vertx.eventBus().<JsonObject>send(
                    AppInstallationVerticle.EVENT_BUS_ADDRESS,
                    installationEvent.toJsonObject(),
                    response -> {
                        
                        JsonObject body = response.result().body();
                        context.assertEquals("ok", body.getString("status"), body.getString("message"));

                        Database.withConnection().setHandler(connected -> {
                            connected.result().query(
                                "SELECT * FROM installations WHERE token = " + tokenResponse.getString("access_token"),
                                result -> {

                                });
                        });

                        async.complete();
                    });
            });
        });
    }
}
