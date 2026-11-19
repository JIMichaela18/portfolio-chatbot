package io.github.jimichaela18.portfoliochatbot.portfolio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public final class PortfolioContext {

    private final String content;

    public PortfolioContext(@Value("classpath:about-me.md") Resource aboutMeResource) throws IOException {
        try (var inputStream = aboutMeResource.getInputStream()) {
            this.content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String content() {
        return content;
    }
}
