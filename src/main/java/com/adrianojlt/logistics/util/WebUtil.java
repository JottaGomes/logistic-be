package com.adrianojlt.logistics.util;

import jakarta.servlet.http.HttpServletRequest;

public final class WebUtil {

    private WebUtil() {}

    public static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
