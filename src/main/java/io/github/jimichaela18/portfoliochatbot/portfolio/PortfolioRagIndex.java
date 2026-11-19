package io.github.jimichaela18.portfoliochatbot.portfolio;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "portfolio.rag.enabled", havingValue = "true")
public class PortfolioRagIndex implements PortfolioReferenceProvider {

    private final EmbeddingStoreContentRetriever contentRetriever;

    @Autowired
    public PortfolioRagIndex(
            PortfolioContext portfolioContext,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            @Value("${portfolio.rag.max-results}") int maxResults,
            @Value("${portfolio.rag.chunk-size}") int chunkSize,
            @Value("${portfolio.rag.chunk-overlap}") int chunkOverlap
    ) {
        this.contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .build();

        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(chunkSize, chunkOverlap))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(Document.from(portfolioContext.content()));
    }

    public String retrieve(String question) {
        return contentRetriever.retrieve(Query.from(question)).stream()
                .map(content -> content.textSegment().text())
                .collect(Collectors.joining("\n\n"));
    }
}