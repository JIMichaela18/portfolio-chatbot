package io.github.jimichaela18.portfoliochatbot.portfolio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public final class PortfolioContext {

    private static final Logger log = LoggerFactory.getLogger(PortfolioContext.class);

    private final String content;

    public PortfolioContext(@Value("classpath:about-me.md") Resource aboutMeResource) throws IOException {
        try (var inputStream = aboutMeResource.getInputStream()) {
            this.content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            log.info("Portfolio knowledge resource loaded: characterCount={}", content.length());
        } catch (IOException exception) {
            log.error("Portfolio knowledge resource could not be loaded", exception);
            throw exception;
        }
    }

    public String content() {
        return content;
    }
}
