package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Installation;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class SnowflakeInstallerVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(AppInstallationVerticle.class);

    public void start() {
        vertx.eventBus().<JsonObject>consumer(AppInstallationVerticle.EVENT_BUS_NOTIFICATION_ADDRESS, message -> {
            Installation appInstallation = Model.fromJsonObject(message.body(), Installation.class);
            uploadScriptTag(appInstallation);
        });
    }

    private Future<JsonObject> uploadScriptTag(Installation appInstallation) {
        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);
        String scriptPath = String.format("%s/%s", appConfig.appStaticPath, "snowflakes.js");
        String scriptUrl = null;
        try {
            String protocol = appConfig.appUseSsl ? "https" : "http";
            scriptUrl = new URL(protocol, appConfig.getFqdn(), appConfig.appPort, scriptPath).toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        JsonObject scriptTagRequest = new JsonObject()
                .put("action", "post-script-tag") //
                .put("apiUrl", appInstallation.apiUrl) //
                .put("token", appInstallation.accessToken) //
                .put("body", new JsonObject().put("scriptUrl", scriptUrl));
        Promise<JsonObject> promise = Promise.promise();
        vertx.eventBus().<JsonObject>request(EpagesApiClientVerticle.EVENT_BUS_ADDRESS, scriptTagRequest, scriptTagResponse -> {
            if (scriptTagResponse.failed()) {
                log.error("could not upload script tag because of '{}'", scriptTagResponse.cause().getMessage());
                promise.fail(scriptTagResponse.cause());
            } else {
                log.info("uploaded script tag to api_url '{}'", appInstallation.apiUrl);
                promise.complete(scriptTagResponse.result().body());
            }
        });
        return promise.future();
    }
}
