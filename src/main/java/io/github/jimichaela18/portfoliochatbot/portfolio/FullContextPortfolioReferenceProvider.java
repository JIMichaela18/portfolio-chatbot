package io.github.jimichaela18.portfoliochatbot.portfolio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "portfolio.rag.enabled", havingValue = "false", matchIfMissing = true)
public class FullContextPortfolioReferenceProvider implements PortfolioReferenceProvider {

    private final PortfolioContext portfolioContext;

    public FullContextPortfolioReferenceProvider(PortfolioContext portfolioContext) {
        this.portfolioContext = portfolioContext;
    }

    @Override
    public String retrieve(String question) {
        return portfolioContext.content();
    }
}

