package de.thokari.epages.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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
        this.tokenPath = "/token";
    }

    public static InstallationRequest fromMultiMap(MultiMap source) {
        return new InstallationRequest(
                source.get("code"), //
                source.get("api_url"), //
                source.get("access_token_url"), //
                source.get("return_url"), //
                source.get("signature"));
    }

    public static InstallationRequest fromCallbackUrl(String callbackUrl) {
        String query = callbackUrl.split("\\?")[1];
        String[] parameters = query.split("&");
        String code = parameters[0].split("=")[1];

        String accessTokenUrl = parameters[1].split("=")[1];
        String urlEncodedSignature = parameters[2].split("=")[1];
        String signature = null;
        try {
            signature = URLDecoder.decode(urlEncodedSignature, "utf-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("something went wrong because of a programming error");
        }

        // TODO why is this missing in the response?
        String apiUrl = accessTokenUrl.substring(0, accessTokenUrl.indexOf("/token"));

        return new InstallationRequest(code, apiUrl, accessTokenUrl, "not_needed", signature);
    }

    @JsonIgnore
    public String calculateSignature(String secret) {
        String algorithm = "HmacSHA256";
        String encoding = "utf-8";
        Mac mac;
        try {
            mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(encoding), algorithm));
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
            LOG.error("signature validation failed because of programming error", e);
            return null;
        }
        byte[] rawSignature = mac.doFinal((code + ":" + accessTokenUrl).getBytes());
        String base64EncodedSignature = Base64.getEncoder().encodeToString(rawSignature);
        LOG.trace("calculated signature '{}' from code '{}' and accessTokenUrl '{}'", base64EncodedSignature, code, accessTokenUrl);
        return base64EncodedSignature;
    }

    @JsonIgnore
    public Boolean hasValidSignature(String secret) {
        String calculatedSignature = calculateSignature(secret);
        return signature.equals(calculatedSignature);
    }
}
