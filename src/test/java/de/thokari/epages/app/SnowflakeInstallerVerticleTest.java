package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Installation;
import de.thokari.epages.app.model.Model;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
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
import java.time.Instant;

@RunWith(VertxUnitRunner.class)
public class SnowflakeInstallerVerticleTest {

    static final String SHOP_NAME = "Test shop";

    static JsonObject configJson;
    static AppConfig appConfig;

    final Integer apiMockPort = 9999;
    final String apiMockUrl = "http://localhost:" + apiMockPort + "/api";

    final JsonObject installationSource = new JsonObject()
            .put("api_url", apiMockUrl)
            .put("access_token", "4HZ9hriF6J3GOnd10JbFzdVehycOvAZf")
            .put("shop_name", SHOP_NAME)
            .put("email_confirmed", false)
            .put("created", Instant.now());
    final Installation installation = Model.fromJsonObject(installationSource, Installation.class);
    final Vertx vertx = Vertx.vertx();
    final DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(configJson);

    MessageConsumer<JsonObject> apiClientMock;

    Promise<JsonObject> uploadRequestReceived = Promise.promise();

    @BeforeClass
    public static void readConfig() throws IOException {
        configJson = new JsonObject(new String(Files.readAllBytes(Paths.get("config.test.json"))));
        appConfig = Model.fromJsonObject(configJson, AppConfig.class);
    }

    @Before
    public void startApiClientMock() {
        apiClientMock = vertx.eventBus().consumer(EpagesApiClientVerticle.EVENT_BUS_ADDRESS, message -> {
            if ("post-script-tag".equals(message.body().getString("action"))) {
                uploadRequestReceived.complete();
            }
        });
    }

    @After
    public void stopApiClientMock(TestContext context) {
        Async async = context.async();
        Promise<Void> apiClientMockClosed = Promise.promise();
        apiClientMock.unregister(apiClientMockClosed);
        apiClientMockClosed.future().setHandler(closed -> async.complete());
        async.awaitSuccess(1000);
    }

    @Test
    public void testSnowflakeScriptInstallation(TestContext context) {
        Async async = context.async();

        // GIVEN

        vertx.deployVerticle(SnowflakeInstallerVerticle.class.getName(), deploymentOpts, deployed -> {
            if (deployed.failed()) {
                deployed.cause().printStackTrace();
                context.fail();
                async.complete();
            }

            // WHEN

            vertx.eventBus().send(AppInstallationVerticle.EVENT_BUS_NOTIFICATION_ADDRESS,
                    installation.toJsonObject());

            // THEN
            uploadRequestReceived.future().setHandler(result -> {
                context.assertTrue(
                        uploadRequestReceived.future().succeeded(),
                        uploadRequestReceived.future().cause() != null ?
                                uploadRequestReceived.future().cause().getMessage() :
                                "<no error message>");
                async.complete();
            });
        });
        async.awaitSuccess(1000);
    }
}
