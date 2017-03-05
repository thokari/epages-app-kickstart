package de.thokari.epages;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;

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

    static String configJson;
    static {
        try {
            configJson = new String(Files.readAllBytes(Paths.get("config.test.json")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    final JsonObject verticleConfig = new JsonObject(configJson);

    final Integer apiMockPort = 9999;
    final String apiMockUrl = "http://localhost:" + apiMockPort + "/api";
    final String apiMockTokenUrl = apiMockUrl + "/token";

    final JsonObject installationEvent = new JsonObject()
        .put("code", "f32ddSbuff2IGAYvtiwYQiyHyuLJWbey")
        .put("api_url", apiMockUrl)
        .put("access_token_url", apiMockTokenUrl)
        .put("return_url", "http://localhost:8080/epages-app");

    final JsonObject tokenResponse = new JsonObject()
        .put("access_token", "4HZ9hriF6J3GOnd10JbFzdVehycOvAZf");

    final Vertx vertx = Vertx.vertx();
    final DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(verticleConfig);

    @Test
    public void testAppInstallationOauthDance(TestContext context) {
        Async async = context.async();
        Future<HttpServer> apiMockStarted = Future.future();

        apiMockStarted.setHandler(started -> {
            context.assertTrue(apiMockStarted.succeeded());

            vertx.deployVerticle(AppInstallationVerticle.class.getName(), deploymentOpts, deployed -> {
                context.assertTrue(deployed.succeeded());

                vertx.eventBus().send(AppInstallationVerticle.EVENT_BUS_ADDRESS, installationEvent, response -> {

                    context.assertEquals("ok", ((JsonObject) response.result().body()).getString("status"));
                    async.complete();
                });
            });
        });

        vertx.createHttpServer().requestHandler(request -> {
            
            context.assertEquals(request.absoluteURI(), apiMockTokenUrl);
            request.response().headers().add("Content-Type", "application/json");
            request.response().end(tokenResponse.toString());
            
        }).listen(apiMockPort, apiMockStarted.completer());
    }
}
