package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EpagesAppMainVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(EpagesAppMainVerticle.class);

    public void start() {
        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);

        DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(appConfig.toJsonObject());

        Future<String> appInstallationDeployed = Future.future();
        Future<String> epagesApiClientDeployed = Future.future();
        Future<String> httpServerDeployed = Future.future();
        Future<String> ssoLoginVerticleDeployed = Future.future();

        vertx.deployVerticle(
            AppInstallationVerticle.class.getName(), deploymentOpts, appInstallationDeployed.completer());
        vertx.deployVerticle(
            HttpServerVerticle.class.getName(), deploymentOpts, httpServerDeployed.completer());
        vertx.deployVerticle(
            EpagesApiClientVerticle.class.getName(), deploymentOpts, epagesApiClientDeployed.completer());
        vertx.deployVerticle(
            SsoLoginVerticle.class.getName(), deploymentOpts, ssoLoginVerticleDeployed.completer());

        CompositeFuture
            .all(
                appInstallationDeployed, //
                httpServerDeployed, //
                epagesApiClientDeployed, //
                ssoLoginVerticleDeployed)
            .setHandler(deployed -> {
                if (deployed.failed()) {
                    throw new RuntimeException("Verticle deployment failed.", deployed.cause());
                } else {
                    LOG.info("App started with config " + appConfig.toJsonObject().encode());
                }
            });
    }

}
