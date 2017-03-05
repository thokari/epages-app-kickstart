package de.thokari.epages;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class EpagesAppMainVerticle extends AbstractVerticle {

    public void start() {

        finalizeConfig(config(), "client_id", "CLIENT_ID");
        finalizeConfig(config(), "client_secret", "CLIENT_SECRET");
        finalizeConfig(config(), "app_protocol", "APP_PROTOCOL");
        finalizeConfig(config(), "app_hostname", "APP_HOSTNAME");
        finalizeConfig(config(), "app_port", "APP_PORT", Integer.class);
        finalizeConfig(config(), "app_static_path", "APP_STATIC_PATH");
        finalizeConfig(config(), "app_api_path", "APP_API_PATH");
        finalizeConfig(config(), "callback_path", "CALLBACK_PATH");

        DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(config());

        Future<String> appInstallationDeployed = Future.future();
        Future<String> httpServerDeployed = Future.future();
        
        vertx.deployVerticle(
            AppInstallationVerticle.class.getName(), deploymentOpts, appInstallationDeployed.completer());
        vertx.deployVerticle(
            HttpServerVerticle.class.getName(), deploymentOpts, httpServerDeployed.completer());

        CompositeFuture.all(appInstallationDeployed, httpServerDeployed).setHandler(deployed -> {
            if (deployed.failed()) {
                throw new RuntimeException("Verticle deployment failed.", deployed.cause());
            } else {
                System.out.println("App started with config " + config().toString());
            }
        });
    }

    private void finalizeConfig(JsonObject config, String key, String envVar) {
        finalizeConfig(config, key, envVar, String.class);
    }

    private void finalizeConfig(JsonObject config, String key, String envVar, Class<?> clazz) {
        String fromEnv = System.getenv(envVar);
        if (fromEnv != null) {
            config.put(key, clazz.cast(fromEnv));
        }
        if (!config().containsKey(key)) {
            throw new RuntimeException("Key '" + key + "' not found in configuration.");
        }
    }
}
