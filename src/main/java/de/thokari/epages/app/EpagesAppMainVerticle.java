package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

public class EpagesAppMainVerticle extends AbstractVerticle {

    public void start() {

        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);

        DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(appConfig.toJsonObject());

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
}
