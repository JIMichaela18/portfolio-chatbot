package io.github.jimichaela18.portfoliochatbot.portfolio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "portfolio.rag.enabled", havingValue = "false", matchIfMissing = true)
public class FullContextPortfolioReferenceProvider implements PortfolioReferenceProvider {

    private static final Logger log = LoggerFactory.getLogger(FullContextPortfolioReferenceProvider.class);

    private final PortfolioContext portfolioContext;

    public FullContextPortfolioReferenceProvider(PortfolioContext portfolioContext) {
        this.portfolioContext = portfolioContext;
    }

    @Override
    public String retrieve(String question) {
        long startedAt = System.nanoTime();
        String reference = portfolioContext.content();
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        log.debug("Full portfolio context retrieved: characterCount={}, elapsedMs={}", reference.length(), elapsedMillis);
        return reference;
    }
}
