package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;

public class EpagesApiClientVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "api_client";

    private HttpClient client;

    public void start() {

        // AppConfig appConfig = Model.fromJsonObject(config(),
        // AppConfig.class);

        client = vertx.createHttpClient();

        vertx.eventBus().<JsonObject>consumer(EVENT_BUS_ADDRESS).handler(message -> {
            JsonObject payload = message.body();
            String action = payload.getString("action");
            String apiUrl = payload.getString("apiUrl");
            switch (action) {
                case ("shop-info") : {
                    client.getNow(apiUrl, response -> {
                        response.bodyHandler(body -> {
                            System.out.println(body);
                        });
                    });

                }

            }

        });

    }
}
