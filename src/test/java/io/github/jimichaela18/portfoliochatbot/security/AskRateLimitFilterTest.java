package io.github.jimichaela18.portfoliochatbot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AskRateLimitFilterTest {

    @Test
    void resetsCounterAfterConfiguredWindowExpires() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AskRateLimitFilter filter = new AskRateLimitFilter(2, Duration.ofMinutes(1), clock);

        assertEquals(200, perform(filter, clock.millis(), "192.0.2.1").getStatus());
        assertEquals(200, perform(filter, clock.millis(), "192.0.2.1").getStatus());
        assertEquals(429, perform(filter, clock.millis(), "192.0.2.1").getStatus());

        clock.advance(Duration.ofMinutes(1));

        assertEquals(200, perform(filter, clock.millis(), "192.0.2.1").getStatus());
        assertEquals(200, perform(filter, clock.millis(), "192.0.2.1").getStatus());
    }

    @Test
    void fixedWindowEnforcesBoundaryExactly() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AskRateLimitFilter filter = new AskRateLimitFilter(1, Duration.ofMinutes(1), clock);

        assertEquals(200, perform(filter, clock.millis(), "192.0.2.2").getStatus());

        clock.advance(Duration.ofMillis(59_999));
        assertEquals(429, perform(filter, clock.millis(), "192.0.2.2").getStatus());

        clock.advance(Duration.ofMillis(1));
        assertEquals(200, perform(filter, clock.millis(), "192.0.2.2").getStatus());
    }

    @Test
    void concurrentRequestsCountExactlyOncePerClient() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AskRateLimitFilter filter = new AskRateLimitFilter(10, Duration.ofMinutes(1), clock);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 20; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return perform(filter, clock.millis(), "192.0.2.3").getStatus();
                }));
            }

            start.countDown();

            AtomicInteger accepted = new AtomicInteger();
            AtomicInteger limited = new AtomicInteger();
            for (Future<Integer> future : futures) {
                int status = future.get(5, TimeUnit.SECONDS);
                if (status == 200) {
                    accepted.incrementAndGet();
                } else if (status == 429) {
                    limited.incrementAndGet();
                }
            }

            assertEquals(10, accepted.get());
            assertEquals(10, limited.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void staleEntryCleanupRemovesInactiveIpAfterTwiceWindow() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AskRateLimitFilter filter = new AskRateLimitFilter(10, Duration.ofMinutes(1), clock);

        assertEquals(200, perform(filter, clock.millis(), "192.0.2.4").getStatus());
        assertEquals(1, filter.trackedIpCount());

        clock.advance(Duration.ofMinutes(2));
        filter.cleanupStaleEntriesForTest(clock.millis());

        assertEquals(0, filter.trackedIpCount());
    }

    @Test
    void staleCleanupDoesNotRemoveActiveRequestWindow() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AskRateLimitFilter filter = new AskRateLimitFilter(10, Duration.ofMinutes(1), clock);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        FilterChain blockingChain = (request, response) -> {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new ServletException(interruptedException);
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future;
        try {
            future = executor.submit(() -> {
                try {
                    perform(filter, blockingChain, "192.0.2.5");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

            entered.await(5, TimeUnit.SECONDS);
            clock.advance(Duration.ofMinutes(2));
            filter.cleanupStaleEntriesForTest(clock.millis());
            assertEquals(1, filter.trackedIpCount());

            release.countDown();
            future.get(5, TimeUnit.SECONDS);
            filter.cleanupStaleEntriesForTest(clock.millis());
            assertEquals(1, filter.trackedIpCount());

            clock.advance(Duration.ofMinutes(2));
            filter.cleanupStaleEntriesForTest(clock.millis());
            assertEquals(0, filter.trackedIpCount());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private MockHttpServletResponse perform(
            AskRateLimitFilter filter,
            long ignoredNow,
            String remoteAddress
    ) throws ServletException, IOException {
        return perform(filter, new MockFilterChain(), remoteAddress);
    }

    private MockHttpServletResponse perform(
            AskRateLimitFilter filter,
            FilterChain chain,
            String remoteAddress
    ) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ask");
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);
        return response;
    }

    private static final class MutableClock extends Clock {
        private volatile Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public long millis() {
            return instant.toEpochMilli();
        }
    }
}
