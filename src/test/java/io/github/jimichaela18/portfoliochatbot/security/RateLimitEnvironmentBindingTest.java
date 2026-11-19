package io.github.jimichaela18.portfoliochatbot.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitEnvironmentBindingTest {

    @Test
    void bindsRenderRateLimitEnvironmentVariables() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        Map.of(
                                "PORTFOLIO_RATE_LIMIT_REQUESTS_PER_MINUTE", "25",
                                "PORTFOLIO_RATE_LIMIT_WINDOW", "30s"
                        )
                )
        );

        Binder binder = Binder.get(environment);

        assertEquals(
                25,
                binder.bind("portfolio.rate-limit.requests-per-minute", Integer.class)
                        .orElseThrow(AssertionError::new)
        );
        assertEquals(
                Duration.ofSeconds(30),
                binder.bind("portfolio.rate-limit.window", Duration.class)
                        .orElseThrow(AssertionError::new)
        );
    }
}
