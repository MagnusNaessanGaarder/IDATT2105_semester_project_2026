package com.example.InternalControl.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Redirects non-versioned API calls to versioned endpoints.
 * Provides backward compatibility while encouraging migration to versioned URLs.
 */
@RestController
@Hidden
public class ApiRedirectController {

    @Value("${api.version:v1}")
    private String apiVersion;

    @RequestMapping(value = "/api/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public void redirectToVersionedApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUri = request.getRequestURI();

        // Skip if already versioned
        if (requestUri.startsWith("/api/" + apiVersion)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        // Build the redirect URL
        String versionedUri = requestUri.replaceFirst("/api/", "/api/" + apiVersion + "/");

        // Append query string if present
        String queryString = request.getQueryString();
        if (queryString != null) {
            versionedUri += "?" + queryString;
        }

        // Send deprecation warning header
        response.addHeader("Deprecation", "true");
        response.addHeader("Sunset", "Sat, 01 Jan 2027 00:00:00 GMT");
        response.addHeader("Warning", "299 - \"API versioning recommended. Use /api/v1/ endpoints.\"");

        // Redirect to versioned endpoint (use 308 to preserve HTTP method)
        response.setStatus(HttpStatus.PERMANENT_REDIRECT.value());
        response.setHeader("Location", versionedUri);
    }
}
