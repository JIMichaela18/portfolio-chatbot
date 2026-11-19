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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "portfolio.rag.enabled", havingValue = "true")
public class PortfolioRagIndex implements PortfolioReferenceProvider {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRagIndex.class);

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
        try {
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

            log.info("Portfolio RAG index initialized: maxResults={}, chunkSize={}, chunkOverlap={}", maxResults, chunkSize, chunkOverlap);
        } catch (RuntimeException exception) {
            log.error("Portfolio RAG index initialization failed", exception);
            throw exception;
        }
    }

    public String retrieve(String question) {
        long startedAt = System.nanoTime();
        try {
            String reference = contentRetriever.retrieve(Query.from(question)).stream()
                    .map(content -> content.textSegment().text())
                    .collect(Collectors.joining("\n\n"));
            log.debug("Portfolio RAG retrieval completed: questionLength={}, referenceLength={}, elapsedMs={}", question.length(), reference.length(), (System.nanoTime() - startedAt) / 1_000_000);
            return reference;
        } catch (RuntimeException exception) {
            log.error("Portfolio RAG retrieval failed", exception);
            throw exception;
        }
    }
}