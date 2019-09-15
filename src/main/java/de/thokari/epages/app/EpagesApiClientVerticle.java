package de.thokari.epages.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class EpagesApiClientVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "api_client";
    private static final Logger LOG = LoggerFactory.getLogger(EpagesApiClientVerticle.class);
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
                case "shop-info":
                    requestUrl = apiUrl;
                    break;
                case "singlesignon":
                    requestUrl = apiUrl + "/singlesignon";
                    break;
            }

            final String finalRequestUrl = requestUrl;

            LOG.info(String.format("Requesting API at '%s'", finalRequestUrl));

            makeApiRequest(finalRequestUrl, token).setHandler(response -> {
                if (response.failed()) {
                    String errorMsg = String.format("API request to '%s' failed because of '%s'", finalRequestUrl,
                            response.cause().getMessage());
                    LOG.error(errorMsg);
                    message.fail(500, errorMsg);
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
            Boolean responseIsOk = String.valueOf(response.statusCode()).startsWith("2")
                    || String.valueOf(response.statusCode()).startsWith("3");
            response.bodyHandler(body -> {
                if (responseIsOk) {
                    future.complete(body.toJsonObject());
                } else {
                    future.fail(body.toString());
                }
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
