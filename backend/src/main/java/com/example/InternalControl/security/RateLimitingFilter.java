package com.example.InternalControl.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting filter using Bucket4j.
 * Limits requests per IP address to prevent brute force and DDoS attacks.
 * Disabled in test profile.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile("!test")
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  // 100 requests per minute for general API
  private static final int API_CAPACITY = 100;
  private static final Duration API_REFILL = Duration.ofMinutes(1);

  // 5 requests per minute for auth endpoints (login, register)
  private static final int AUTH_CAPACITY = 5;
  private static final Duration AUTH_REFILL = Duration.ofMinutes(1);

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    String clientIp = getClientIp(request);
    String path = request.getRequestURI();

    Bucket bucket = buckets.computeIfAbsent(clientIp + path, k -> createBucket(path));

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      // Add rate limit headers
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
    } else {
      long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
      log.warn("Rate limit exceeded for IP: {} on path: {}, wait time: {}s", clientIp, path, waitForRefill);

      response.setStatus(429);
      response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
    }
  }

  private Bucket createBucket(String path) {
    Bandwidth bandwidth;

    // Stricter limits for auth endpoints
    if (path.contains("/auth/login") || path.contains("/auth/register")) {
      bandwidth = Bandwidth.classic(AUTH_CAPACITY, Refill.intervally(AUTH_CAPACITY, AUTH_REFILL));
    } else {
      bandwidth = Bandwidth.classic(API_CAPACITY, Refill.intervally(API_CAPACITY, API_REFILL));
    }

    return Bucket.builder()
        .addLimit(bandwidth)
        .build();
  }

  private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null) {
      return request.getRemoteAddr();
    }
    return xfHeader.split(",")[0].trim();
  }
}
