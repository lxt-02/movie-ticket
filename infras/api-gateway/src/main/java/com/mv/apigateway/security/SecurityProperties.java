package com.mv.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.security.*}: where the Keycloak realm is. */
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** Must equal the "iss" claim of the tokens, e.g. http://localhost:8180/realms/mss301. */
    private String issuerUri = "http://localhost:8180/realms/mss301";

    /** Public keys of the realm. Empty = {issuerUri}/protocol/openid-connect/certs. */
    private String jwkSetUri;

    /** Allowed browser origins (same list as the gateway CORS config), comma-separated. */
    private String allowedOrigins = "http://localhost:3000";

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri == null || jwkSetUri.isBlank() ? issuerUri + "/protocol/openid-connect/certs" : jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
