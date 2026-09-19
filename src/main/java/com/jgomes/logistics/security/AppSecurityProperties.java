package com.jgomes.logistics.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private boolean enableLogin;

    private List<String> corsAllowedOrigins = new ArrayList<>();

    public boolean isEnableLogin() {
        return enableLogin;
    }

    public void setEnableLogin(boolean enableLogin) {
        this.enableLogin = enableLogin;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }
}
