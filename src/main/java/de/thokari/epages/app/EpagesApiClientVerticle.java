package de.thokari.epages.app;

import java.net.MalformedURLException;
import java.net.URL;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EpagesApiClientVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "api_client";
    private static final Logger LOG = LoggerFactory.getLogger(AppInstallationVerticle.class);
    private HttpClient client;

    public void start() {

        client = vertx.createHttpClient();

        vertx.eventBus().<JsonObject>consumer(EVENT_BUS_ADDRESS).handler(message -> {
            JsonObject payload = message.body();

            String action = payload.getString("action");
            String apiUrl = payload.getString("apiUrl");
            String token = payload.getString("token");

            String requestUrl = null;
            switch (action) {
                case "shop-info" :
                    requestUrl = apiUrl;
                    break;
            }

            makeApiRequest(requestUrl, token).setHandler(response -> {
                if (response.failed()) {
                    String errorMessage = "API request failed";
                    LOG.error(errorMessage, response.cause());
                    message.fail(500, errorMessage);
                } else {
                    message.reply(response.result());
                }
            });

        });
    }

    @SuppressWarnings("unused")
    private Future<JsonObject> makeApiRequest(String apiUrl) {
        return makeApiRequest(apiUrl, null);
    }

    private Future<JsonObject> makeApiRequest(String apiUrl, String token) {
        Future<JsonObject> future = Future.future();

        URL url = null;
        try {
            url = new URL(apiUrl);
        } catch (MalformedURLException malformedUrl) {
            future.fail(malformedUrl);
        }

        boolean useSsl = "https".equals(url.getProtocol());
        RequestOptions options = new RequestOptions()
            .setSsl(useSsl)
            .setPort(url.getPort() != -1 ? url.getPort() : (useSsl ? 443 : 80))
            .setHost(url.getHost())
            .setURI(url.getPath());

        HttpClientRequest request = client.get(options, response -> {
            response.exceptionHandler(exception -> future.fail(exception));
            response.bodyHandler(body -> {
                future.complete(body.toJsonObject());
            });
        });
        if (token != null) {
            request.headers().add("Authorization", "Bearer " + token);
        }
        request.exceptionHandler(exception -> future.fail(exception));
        request.end();

        return future;
    }
}
