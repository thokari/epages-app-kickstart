package de.thokari.epages.app;

import de.thokari.epages.app.model.AppConfig;
import de.thokari.epages.app.model.InstallationRequest;
import de.thokari.epages.app.model.Model;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SsoShopCreationVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ADDRESS = "sso_create_shop";

    private static final Logger LOG = LoggerFactory.getLogger(SsoShopCreationVerticle.class);

    public void start() {

        AppConfig appConfig = Model.fromJsonObject(config(), AppConfig.class);
        vertx.eventBus().<String>consumer(EVENT_BUS_ADDRESS).handler(handleCreationEvent(appConfig.clientId));
    }

    private Handler<Message<String>> handleCreationEvent(String clientId) {
        return message -> {

            String[] parameters = message.body().split("&");
            String epagesHostname = parameters[0].split("=")[1];
            String shopName = parameters[1].split("=")[1];
            LOG.info(String.format("received creation request for shop '%s' on host '%s'", shopName, epagesHostname));

            DatagramSocket datagramSocket = vertx.createDatagramSocket();
            datagramSocket.handler(handleShopGuidResponse(clientId, datagramSocket, message));
            sendShopGuidRequest(epagesHostname, shopName, datagramSocket);
        };
    }

    private Handler<DatagramPacket> handleShopGuidResponse(String clientId, DatagramSocket socket,
        Message<String> message) {

        return packet -> {
            String shopGuid = packet.data().toString().split("\\s")[10];
            LOG.error(
                String.format("successfully queried shop GUID '%s'", shopGuid));
            socket.close();

            String appInstallationUrl = String.format(
                "/rs/appstore/%s/installations/%s?businessUnit=Distributor&shopType=Demo&scope=sso_read&locale=de_DE",
                shopGuid, clientId);
            LOG.info(String.format("requesting installation on host '%s' using URL '%s'", "localhost",
                appInstallationUrl));

            performInstallationReplyingToCreationMessage(appInstallationUrl, message);
        };
    }

    private void sendShopGuidRequest(String epagesHostname, String shopName, DatagramSocket socket) {
        String payload = String.format("abc REQ %s /epages/%s. rest", epagesHostname,
            shopName);
        socket.send(payload, 17277, epagesHostname, socketResult -> {
            if (socketResult.failed()) {
                LOG.error(
                    String.format("could not query shop GUID because of '%s'", socketResult.cause().getMessage()));
            }
        });
    }

    private void performInstallationReplyingToCreationMessage(String appInstallationUrl, Message<String> message) {
        RequestOptions reqOptions = new RequestOptions() //
            .setSsl(false) //
            .setHost("localhost") //
            .setPort(8088) //
            .setURI(appInstallationUrl);

        HttpClientRequest request = vertx.createHttpClient().post(reqOptions);
        request.handler(response -> {
            response.bodyHandler(bodyBuffer -> {
                int statusCode = response.statusCode();
                int expectedStatusCode = 202;
                String body = bodyBuffer.toString();
                if (!(statusCode == expectedStatusCode)) {
                    LOG.error(String.format("Expected status code %d, but got %d, response was '%s'",
                        expectedStatusCode, statusCode, body));
                    message.fail(500, body);
                } else {
                    JsonObject jsonBody = new JsonObject(body);

                    InstallationRequest event = InstallationRequest
                        .fromCallbackUrl(jsonBody.getString("callbackUrl"));

                    vertx.eventBus().<JsonObject>send(
                        AppInstallationVerticle.EVENT_BUS_ADDRESS, event.toJsonObject(), reply -> {
                            if (reply.failed()) {
                                ReplyException error = (ReplyException) reply.cause();
                                String errorMsg = error.getMessage();
                                int installationStatusCode = error.failureCode();
                                message.fail(installationStatusCode, errorMsg);
                            } else {
                                message.reply(reply.result().body());
                            }
                        });
                }
            });
        });
        request.end();
    }
}
