package de.thokari.epages.app;

import static io.vertx.core.http.HttpMethod.GET;

import java.net.MalformedURLException;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.InstallationRequest;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class HttpServerVerticle extends AbstractVerticle {

    public void start() throws MalformedURLException {

        final AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);

        Router mainRouter = Router.router(vertx);
        mainRouter.route(GET, appConfig.callbackPath).handler(ctx -> {

            final HttpServerResponse response = ctx.response();
            InstallationRequest event = null;

            try {
                event = InstallationRequest.fromMultiMap(ctx.request().params());
                vertx.eventBus().<JsonObject>send(
                    AppInstallationVerticle.EVENT_BUS_ADDRESS, event.toJsonObject(), reply -> {

                        JsonObject result = reply.result().body();
                        if ("error".equals(result.getString("status"))) {
                            response.setStatusCode(500).end(result.getString("message"));
                        } else {
                            response.headers().add("Location", appConfig.appStaticPath);
                            response.setStatusCode(302).end();
                        }
                    });
            } catch (IllegalArgumentException e) {
                response.setStatusCode(400).end(e.getMessage());
            }
        });

        Router apiRouter = Router.router(vertx);
        apiRouter.route(GET, "/*").handler(ctx -> {
            ctx.response().end("version: 0.1");
        });

        mainRouter.mountSubRouter(appConfig.appApiPath, apiRouter);
        mainRouter.route(appConfig.appStaticPath).handler(StaticHandler.create());

        PemKeyCertOptions certOptions = new PemKeyCertOptions();

        HttpServerOptions serverOptions = new HttpServerOptions()
            .setSsl("https" == appConfig.appProtocol)
            .setPemKeyCertOptions(certOptions);

        HttpServer server = vertx
            .createHttpServer(serverOptions);
        server.requestHandler(mainRouter::accept).listen(appConfig.appPort, appConfig.appHostname);
    }

}
