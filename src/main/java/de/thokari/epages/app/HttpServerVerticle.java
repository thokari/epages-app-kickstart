package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.InstallationRequest;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;

import static io.vertx.core.http.HttpMethod.GET;

public class HttpServerVerticle extends AbstractVerticle {

    public void start() {

        final AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);

        Router mainRouter = Router.router(vertx);
        mainRouter.route().handler(LoggerHandler.create());
        mainRouter.route().handler(BodyHandler.create());

        mainRouter.route(GET, appConfig.callbackPath).handler(ctx -> {

            final HttpServerResponse response = ctx.response();
            InstallationRequest event = null;

            try {
                event = InstallationRequest.fromMultiMap(ctx.request().params());
            } catch (IllegalArgumentException e) {
                response.setStatusCode(400).end(e.getMessage());
            }

            if (event != null) {
                vertx.eventBus().<JsonObject>send(
                    AppInstallationVerticle.EVENT_BUS_ADDRESS, event.toJsonObject(), reply -> {

                        if (reply.failed()) {
                            ReplyException error = (ReplyException) reply.cause();
                            String errorMsg = error.getMessage();
                            int statusCode = error.failureCode();
                            response.setStatusCode(statusCode).end(errorMsg);
                        } else {
                            response.setStatusCode(204).end();
                        }
                    });
            }
        });

        Router apiRouter = Router.router(vertx);
        apiRouter.route(GET, "/*").handler(ctx -> {
            ctx.response().end("version: 0.1");
        });

        mainRouter.mountSubRouter(appConfig.appApiPath, apiRouter);

        StaticHandler staticHandler = StaticHandler.create().setMaxAgeSeconds(0);
        mainRouter.route(appConfig.appStaticPath + "/*").handler(staticHandler);

        HttpServerOptions serverOptions = new HttpServerOptions();
        if ("https".equals(appConfig.appProtocol)) {
            SelfSignedCertificate certificate = SelfSignedCertificate.create();
            serverOptions.setSsl(true).setKeyCertOptions(certificate.keyCertOptions());
        }
        HttpServer server = vertx.createHttpServer(serverOptions);

        server.requestHandler(mainRouter::accept).listen(appConfig.appPort, appConfig.appHostname);
    }
}
