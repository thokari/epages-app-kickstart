package de.thokari.epages;

import static io.vertx.core.http.HttpMethod.GET;

import java.net.MalformedURLException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class HttpServerVerticle extends AbstractVerticle {

    public void start() throws MalformedURLException {

        final String appProtocol = config().getString("app_protocol");
        final String appHostname = config().getString("app_hostname");
        final Integer appPort = config().getInteger("app_port");
        final String appStaticPath = config().getString("app_static_path");
        final String appApiPath = config().getString("app_api_path");
        final String callbackPath = config().getString("callback_path");

        Router mainRouter = Router.router(vertx);
        mainRouter.route(GET, callbackPath).handler(ctx -> {

            MultiMap query = ctx.request().params();
            final String code = query.get("code");
            final String returnUrl = query.get("return_url");
            final String apiUrl = query.get("api_url");
            final String accessTokenUrl = query.get("access_token_url");
            final HttpServerResponse response = ctx.response();

            if (!(code != null && returnUrl != null && apiUrl != null && accessTokenUrl != null)) {
                response.setStatusCode(400).end("invalid parameters");
            } else {
                JsonObject message = new JsonObject()
                    .put("code", code)
                    .put("api_url", apiUrl)
                    .put("access_token_url", accessTokenUrl);
                vertx.eventBus().send(AppInstallationVerticle.EVENT_BUS_ADDRESS, message, reply -> {
                    System.out.println(reply.result().body().toString());
                    response.headers().add("Location", appStaticPath);
                    response.setStatusCode(302).end();
                });
            }
        });

        Router apiRouter = Router.router(vertx);
        apiRouter.route(GET, "/*").handler(ctx -> {
            ctx.response().end("version: 0.1");
        });

        mainRouter.mountSubRouter(appApiPath, apiRouter);
        mainRouter.route(appStaticPath).handler(StaticHandler.create());

        PemKeyCertOptions certOptions = new PemKeyCertOptions();

        HttpServerOptions serverOptions = new HttpServerOptions()
            .setSsl("https" == appProtocol)
            .setPemKeyCertOptions(certOptions);

        HttpServer server = vertx.createHttpServer(serverOptions);
        server.requestHandler(mainRouter::accept).listen(appPort, appHostname);
    }

}
