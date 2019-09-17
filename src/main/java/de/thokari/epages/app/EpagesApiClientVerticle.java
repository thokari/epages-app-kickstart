package de.thokari.epages.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            JsonObject body = payload.getJsonObject("body");

            String requestUrl = null;
            String contentType = null;
            HttpMethod httpMethod = HttpMethod.GET;
            switch (action) {
                case "get-shop-info":
                    requestUrl = apiUrl;
                    break;
                case "post-script-tag":
                    requestUrl = apiUrl + "/script-tags";
                    httpMethod = HttpMethod.POST;
                    contentType = "application/json";
                    break;
            }

            final String finalRequestUrl = requestUrl;

            LOG.info("requesting API at '{}'", finalRequestUrl);

            makeApiRequest(httpMethod, finalRequestUrl, token, body, contentType).setHandler(response -> {
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

    private Future<JsonObject> makeApiRequest(HttpMethod method, String apiUrl, String token, Object postBody, String contentType) {
        Promise<JsonObject> promise = Promise.promise();

        URL url = null;
        try {
            url = new URL(apiUrl);
        } catch (MalformedURLException malformedUrl) {
            promise.fail(malformedUrl);
        }

        boolean useSsl = "https".equals(url.getProtocol());
        RequestOptions options = new RequestOptions()
                .setSsl(useSsl)
                .setPort(url.getPort() != -1 ? url.getPort() : (useSsl ? 443 : 80))
                .setHost(url.getHost())
                .setURI(url.getPath());

        HttpClientRequest request = client.request(method, options);
        request.handler(response -> {
            response.exceptionHandler(promise::fail);
            Boolean responseIsOk = String.valueOf(response.statusCode()).startsWith("2")
                    || String.valueOf(response.statusCode()).startsWith("3");
            response.bodyHandler(body -> {
                LOG.debug("received response code '{}', body '{}'", response.statusCode(), body.toString());
                if (responseIsOk) {
                    promise.complete(body.toJsonObject());
                } else {
                    promise.fail(body.toString());
                }
            });
        });
        if (HttpMethod.POST == method) {
            request.headers().add("Content-Length", String.valueOf(postBody.toString().length()));
            request.write(postBody.toString());
            if (contentType != null) {
                request.headers().add("Content-Type", contentType);
            }
        }
        if (token != null) {
            request.headers().add("Authorization", "Bearer " + token);
        }
        request.exceptionHandler(promise::fail);
        request.end();

        return promise.future();
    }
}