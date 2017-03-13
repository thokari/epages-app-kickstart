package de.thokari.epages.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InstallationRequest extends Model {
    
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

    @JsonCreator
    public InstallationRequest(
        @JsonProperty("code") String code,
        @JsonProperty("api_url") String apiUrl,
        @JsonProperty("access_token_url") String accessTokenUrl,
        @JsonProperty("return_url") String returnUrl) {
        
        this.code = validate("code", code);
        this.apiUrl = validate("api_url", apiUrl); // TODO check API URL pattern!!!!!!!
        this.accessTokenUrl = validate("access_token_url", accessTokenUrl);
        this.returnUrl = validate("return_url", returnUrl);
        this.tokenPath = accessTokenUrl.substring(apiUrl.length());
    }
}
