package de.thokari.epages.app.model;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class InstallationRequest extends Model {

    private static final Logger LOG = LoggerFactory.getLogger(InstallationRequest.class);

    @JsonProperty("code")
    public String code;

    @JsonProperty("api_url")
    public String apiUrl;

    @JsonProperty("access_token_url")
    public String accessTokenUrl;

    @JsonProperty("return_url")
    public String returnUrl;

    @JsonProperty("token_path")
    public String tokenPath;

    @JsonProperty("signature")
    public String signature;

    @JsonCreator
    public InstallationRequest(
        @JsonProperty("code") String code,
        @JsonProperty("api_url") String apiUrl,
        @JsonProperty("access_token_url") String accessTokenUrl,
        @JsonProperty("return_url") String returnUrl,
        @JsonProperty("signature") String signature) {

        this.code = validate("code", code);
        this.apiUrl = validate("api_url", apiUrl);
        this.accessTokenUrl = validate("access_token_url", accessTokenUrl);
        this.returnUrl = validate("return_url", returnUrl);
        this.signature = validate("signature", signature);
        try {
            this.tokenPath = accessTokenUrl.substring(apiUrl.length());
        } catch (Exception e) {
            throw new IllegalArgumentException("access_token_url must contain api_url");
        }
    }

    public static InstallationRequest fromMultiMap(MultiMap source) {
        return new InstallationRequest(
            source.get("code"),
            source.get("api_url"),
            source.get("access_token_url"),
            source.get("return_url"),
            source.get("signature"));
    }

    public Boolean hasValidSignature(String secret) {
        String algorithm = "HmacSHA256";
        String encoding = "utf-8";
        Mac mac;
        try {
            mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(encoding), algorithm));
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
            LOG.error("Signature validation failed because of programming error", e);
            return false;
        }

        byte[] rawSignature = mac.doFinal((this.code + ":" + this.accessTokenUrl).getBytes());
        String signature = Base64.getEncoder().encodeToString(rawSignature);

        return this.signature.equals(signature);
    }
}
