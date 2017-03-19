package de.thokari.epages.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;

public class EpagesApiClientVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "api_client";

    private HttpClient client;

    public void start() {

        // AppConfig appConfig = Model.fromJsonObject(config(),
        // AppConfig.class);

        client = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setDefaultPort(443));

        vertx.eventBus().<JsonObject>consumer(EVENT_BUS_ADDRESS).handler(message -> {
            JsonObject payload = message.body();
            String action = payload.getString("action");
            String apiUrl = payload.getString("apiUrl");
            String token = payload.getString("token");
            switch (action) {
                case ("shop-info") : {
                    System.out.println(apiUrl);
                    client.getNow("devshop.epages.com", "/rs/shops/epagesdevD20161020T212339R164/", response -> {
                        response.bodyHandler(body -> {
                            System.out.println(body);
                            message.reply(body);

                        });
                    });

                    // request.headers()
                    // .add("Authorization", "Bearer " + token);
                    // .add("");
                    

                }

            }

        });

    }
}
