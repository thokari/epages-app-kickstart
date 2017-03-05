package de.thokari.epages;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AppInstallationVerticleTest {

    final String appProtocol = "http";
    final String appHostname = "localhost";
    final Integer appPort = 8080;
    final String appPath = "/my-app";
    final String callbackPath = "/callback";
    final String returnUrl = appProtocol + "://" + appHostname + appPath;

    final Integer apiMockPort = 9999;
    final String apiMockUrl = "http://localhost:" + apiMockPort + "/api";
    final String apiMockTokenUrl = apiMockUrl + "/token";

    final JsonObject verticleConfig = new JsonObject()
        .put("client_id", "my-client-id")
        .put("client_secret", "my-client-secret")
        .put("app_protocol", appProtocol)
        .put("app_hostname", appHostname)
        .put("app_port", appPort)
        .put("callback_path", callbackPath);

    final JsonObject installationEvent = new JsonObject()
        .put("code", "some-auth-code")
        .put("api_url", apiMockUrl)
        .put("access_token_url", apiMockTokenUrl)
        .put("return_url", returnUrl);

    final JsonObject tokenResponse = new JsonObject()
        .put("access_token", "4HZ9hriF6J3GOnd10JbFzdVehycOvAZf");

    final Vertx vertx = Vertx.vertx();
    final DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(verticleConfig);

    @Test
    public void testAppInstallationOauthDance(TestContext context) {
        Async async = context.async();

        vertx.createHttpServer().requestHandler(request -> {

            context.assertEquals(request.absoluteURI(), apiMockTokenUrl);
            request.response().headers().add("Content-Type", "application/json");
            request.response().end(tokenResponse.toString());

        }).listen(apiMockPort, apiMockListening -> {
            context.assertTrue(apiMockListening.succeeded());

            vertx.deployVerticle(AppInstallationVerticle.class.getName(), deploymentOpts, deployed -> {
                context.assertTrue(deployed.succeeded());

                vertx.eventBus().send(AppInstallationVerticle.EVENT_BUS_ADDRESS, installationEvent, response -> {

                    context.assertEquals("ok", ((JsonObject) response.result().body()).getString("status"));
                    async.complete();
                });
            });
        });
    }
}
