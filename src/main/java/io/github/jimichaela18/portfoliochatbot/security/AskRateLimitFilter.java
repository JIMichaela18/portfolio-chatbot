package io.github.jimichaela18.portfoliochatbot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AskRateLimitFilter extends OncePerRequestFilter {

    private final int requestLimit;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Window> requestsByIp = new ConcurrentHashMap<>();

    @Autowired
    public AskRateLimitFilter(
            @Value("${portfolio.rate-limit.requests-per-minute}") int requestLimit,
            @Value("${portfolio.rate-limit.window}") Duration window
    ) {
        this(requestLimit, window, Clock.systemUTC());
    }

    AskRateLimitFilter(int requestLimit, Duration window, Clock clock) {
        this.requestLimit = requestLimit;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !"/ask".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = clock.millis();
        String clientIp = clientIp(request);
        Window window = windowFor(clientIp, now);

        try {
            synchronized (window) {
                if (now - window.startedAt >= windowMillis) {
                    window.startedAt = now;
                    window.count = 0;
                }

                if (window.count >= requestLimit) {
                    long retryAfterSeconds = Math.max(
                            1,
                            (windowMillis - (now - window.startedAt) + 999) / 1000
                    );
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Too many requests\"}");
                    window.lastSeenAt = now;
                    return;
                }

                window.count++;
                window.lastSeenAt = now;
            }

            filterChain.doFilter(request, response);
        } finally {
            synchronized (window) {
                window.activeRequests--;
                window.lastSeenAt = clock.millis();
            }
            cleanupStaleEntries(clock.millis());
        }
    }

    private Window windowFor(String clientIp, long now) {
        synchronized (requestsByIp) {
            Window window = requestsByIp.computeIfAbsent(
                    clientIp,
                    ignored -> new Window(now)
            );

            synchronized (window) {
                window.activeRequests++;
                window.lastSeenAt = now;
            }

            cleanupStaleEntriesLocked(now);
            return window;
        }
    }

    void cleanupStaleEntriesForTest(long now) {
        cleanupStaleEntries(now);
    }

    private void cleanupStaleEntries(long now) {
        synchronized (requestsByIp) {
            cleanupStaleEntriesLocked(now);
        }
    }

    private void cleanupStaleEntriesLocked(long now) {
        long staleAgeMillis = windowMillis * 2;
        requestsByIp.forEach((clientIp, window) -> {
            synchronized (window) {
                if (window.activeRequests == 0
                        && now - window.lastSeenAt >= staleAgeMillis) {
                    requestsByIp.remove(clientIp, window);
                }
            }
        });
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    int trackedIpCount() {
        synchronized (requestsByIp) {
            return requestsByIp.size();
        }
    }

    private static final class Window {
        private long startedAt;
        private int count;
        private int activeRequests;
        private long lastSeenAt;

        private Window(long startedAt) {
            this.startedAt = startedAt;
            this.lastSeenAt = startedAt;
        }
    }
}
