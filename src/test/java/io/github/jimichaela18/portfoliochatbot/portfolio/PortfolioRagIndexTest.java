package io.github.jimichaela18.portfoliochatbot.portfolio;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "GQ_LLM_API_KEY=test-key",
        "portfolio.rag.enabled=true"
})
class PortfolioRagIndexTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PortfolioRagIndex portfolioRagIndex;

    @MockitoSpyBean(name = "portfolioEmbeddingStore")
    private EmbeddingStore<TextSegment> embeddingStore;

    //Version B
    @Test
    void retrievesRelevantPortfolioChunk() throws Exception {
        String content = "Technical skills include Java, Spring Boot, and REST API development.";
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8)
        ));

        PortfolioRagIndex ragIndex = new PortfolioRagIndex(
                new PortfolioContext(resource),
                new AllMiniLmL6V2EmbeddingModel(),
                new InMemoryEmbeddingStore<>(),
                4,
                500,
                50
        );

        assertTrue(ragIndex.retrieve("What technical skills are listed?").contains(content));
    }

    @Test
    void ingestsPortfolioOncePerSpringContextAndReusesIndexForRetrieval() {
        assertSame(portfolioRagIndex, applicationContext.getBean(PortfolioRagIndex.class));

        portfolioRagIndex.retrieve("What technical skills are listed?");
        portfolioRagIndex.retrieve("What technical skills are listed?");

        verify(embeddingStore, times(1)).addAll(
                anyList(),
                anyList(),
                argThat(segments -> segments.size() > 1)
        );
    }
}