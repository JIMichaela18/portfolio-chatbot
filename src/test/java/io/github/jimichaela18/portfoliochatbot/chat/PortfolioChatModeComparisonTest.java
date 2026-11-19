package io.github.jimichaela18.portfoliochatbot.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import io.github.jimichaela18.portfoliochatbot.portfolio.FullContextPortfolioReferenceProvider;
import io.github.jimichaela18.portfoliochatbot.portfolio.PortfolioContext;
import io.github.jimichaela18.portfoliochatbot.portfolio.PortfolioRagIndex;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortfolioChatModeComparisonTest {

    private static final String UNKNOWN_ANSWER =
            "Salary expectations are not listed; contact her by email.";

    @Test
    void comparesFullContextAndRetrievedContextForSamePortfolioQuestions() throws Exception {
        PortfolioContext portfolioContext = new PortfolioContext(new ClassPathResource("about-me.md"));
        PortfolioRagIndex ragIndex = new PortfolioRagIndex(
                portfolioContext,
                new AllMiniLmL6V2EmbeddingModel(),
                new InMemoryEmbeddingStore<>(),
                4,
                500,
                50
        );
        List<QuestionCase> cases = List.of(
                new QuestionCase(
                        "Which languages are listed: Java 11, SQL, JavaScript, HTML, CSS?",
                        "Java 11",
                        "Her listed skills include Java 11, SQL, JavaScript, and Spring Boot."
                ),
                new QuestionCase(
                        "What is the digi-content billing and subscription project that uses Kafka?",
                        "production-style digital billing and subscription platform",
                        "digi-content is a production-style digital billing and subscription platform using Kafka."
                ),
                new QuestionCase(
                        "What happened in the May 2023 relocation career break?",
                        "career break since May 2023 because of a relocation",
                        "Her career break began in May 2023 because of a relocation."
                ),
                new QuestionCase(
                        "What salary expectations are listed here?",
                        "Salary expectations are not listed here.",
                        UNKNOWN_ANSWER
                )
        );

        ChatModel versionAChatModel = mockChatModel();
        ChatModel versionBChatModel = mockChatModel();
        PortfolioChatService versionA = new PortfolioChatService(
                versionAChatModel,
                new FullContextPortfolioReferenceProvider(portfolioContext)
        );
        PortfolioChatService versionB = new PortfolioChatService(
                versionBChatModel,
                ragIndex
        );

        List<String> versionAAnswers = cases.stream()
                .map(question -> versionA.answerQuestion(question.question()))
                .toList();
        List<String> versionBAnswers = cases.stream()
                .map(question -> versionB.answerQuestion(question.question()))
                .toList();

        List<ChatMessage[]> versionARequests = captureRequests(versionAChatModel, cases.size());
        List<ChatMessage[]> versionBRequests = captureRequests(versionBChatModel, cases.size());
        for (int i = 0; i < cases.size(); i++) {
            QuestionCase question = cases.get(i);
            SystemMessage versionASystem = assertInstanceOf(SystemMessage.class, versionARequests.get(i)[0]);
            UserMessage versionAUser = assertInstanceOf(UserMessage.class, versionARequests.get(i)[1]);
            SystemMessage versionBSystem = assertInstanceOf(SystemMessage.class, versionBRequests.get(i)[0]);
            UserMessage versionBUser = assertInstanceOf(UserMessage.class, versionBRequests.get(i)[1]);

            assertEquals(question.question(), versionAUser.singleText());
            assertEquals(question.question(), versionBUser.singleText());
            assertTrue(versionASystem.text().contains(portfolioContext.content()));

            String versionBReference = referenceFrom(versionBSystem);
            assertTrue(versionBReference.contains(question.expectedFact()),
                    () -> question.question() + "\nRetrieved context:\n" + versionBReference);
            assertTrue(versionBReference.length() < portfolioContext.content().length());
            assertFalse(versionBReference.equals(portfolioContext.content()));

            assertEquals(question.expectedAnswer(), versionAAnswers.get(i));
            assertEquals(question.expectedAnswer(), versionBAnswers.get(i));
            assertEquals(versionAAnswers.get(i), versionBAnswers.get(i));
        }

        assertEquals(UNKNOWN_ANSWER, versionAAnswers.get(cases.size() - 1));
        assertEquals(UNKNOWN_ANSWER, versionBAnswers.get(cases.size() - 1));
    }

    private static ChatModel mockChatModel() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.chat(any(ChatMessage[].class))).thenAnswer(invocation -> {
            SystemMessage systemMessage = invocation.getArgument(0);
            String question = ((UserMessage) invocation.getArgument(1)).singleText();
            String answer = switch (question) {
                case "Which languages are listed: Java 11, SQL, JavaScript, HTML, CSS?" ->
                        systemMessage.text().contains("Java 11")
                                ? "Her listed skills include Java 11, SQL, JavaScript, and Spring Boot."
                                : UNKNOWN_ANSWER;
                case "What is the digi-content billing and subscription project that uses Kafka?" ->
                        systemMessage.text().contains("production-style digital billing and subscription platform")
                                ? "digi-content is a production-style digital billing and subscription platform using Kafka."
                                : UNKNOWN_ANSWER;
                case "What happened in the May 2023 relocation career break?" ->
                        systemMessage.text().contains("career break since May 2023 because of a relocation")
                                ? "Her career break began in May 2023 because of a relocation."
                                : UNKNOWN_ANSWER;
                case "What salary expectations are listed here?" ->
                        systemMessage.text().contains("Salary expectations are not listed here.")
                                ? UNKNOWN_ANSWER
                                : "I don't have that information.";
                default -> throw new IllegalArgumentException("Unexpected comparison question: " + question);
            };
            return ChatResponse.builder().aiMessage(AiMessage.from(answer)).build();
        });
        return chatModel;
    }

    private static List<ChatMessage[]> captureRequests(ChatModel chatModel, int requestCount) {
        ArgumentCaptor<ChatMessage[]> requests = ArgumentCaptor.forClass(ChatMessage[].class);
        verify(chatModel, times(requestCount)).chat(requests.capture());
        return requests.getAllValues();
    }

    private static String referenceFrom(SystemMessage systemMessage) {
        String text = systemMessage.text();
        String begin = "--- BEGIN PORTFOLIO REFERENCE ---\n";
        String end = "\n--- END PORTFOLIO REFERENCE ---";
        int start = text.indexOf(begin) + begin.length();
        int finish = text.indexOf(end, start);
        return text.substring(start, finish);
    }

    private record QuestionCase(String question, String expectedFact, String expectedAnswer) {
    }
}