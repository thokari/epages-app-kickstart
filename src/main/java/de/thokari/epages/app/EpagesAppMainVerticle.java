package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpagesAppMainVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(EpagesAppMainVerticle.class);

    public void start() {

        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);

        DeploymentOptions deploymentOpts = new DeploymentOptions().setConfig(appConfig.toJsonObject());

        Promise<String> appInstallationDeployed = Promise.promise();
        Promise<String> epagesApiClientDeployed = Promise.promise();
        Promise<String> httpServerDeployed = Promise.promise();

        vertx.deployVerticle(
                AppInstallationVerticle.class.getName(), deploymentOpts, appInstallationDeployed);
        vertx.deployVerticle(
                HttpServerVerticle.class.getName(), deploymentOpts, httpServerDeployed);
        vertx.deployVerticle(
                EpagesApiClientVerticle.class.getName(), deploymentOpts, epagesApiClientDeployed);

        CompositeFuture.all(
                appInstallationDeployed.future(), //
                httpServerDeployed.future(), //
                epagesApiClientDeployed.future()
        ).setHandler(deployed -> {
            if (deployed.failed()) {
                throw new RuntimeException("Verticle deployment failed.", deployed.cause());
            } else {
                LOG.trace("logging at 'TRACE' level");
                LOG.debug("logging at 'DEBUG' level");
                LOG.info("logging at 'INFO' level");
                LOG.info("started with config '{}'", appConfig.toJsonObject().encode());
            }
        });
    }
}
