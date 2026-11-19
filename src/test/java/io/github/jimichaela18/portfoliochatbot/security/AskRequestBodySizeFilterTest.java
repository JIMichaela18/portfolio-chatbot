package io.github.jimichaela18.portfoliochatbot.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AskRequestBodySizeFilterTest {

    private final AskRequestBodySizeFilter filter = new AskRequestBodySizeFilter();

    @Test
    void acceptsRequestBodyAtSixteenKilobyteBoundary() throws Exception {
        byte[] body = bodyWithSize(AskRequestBodySizeFilter.MAX_REQUEST_BODY_BYTES);
        MockHttpServletRequest request = request(body);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsRequestBodyAboveSixteenKilobyteBoundary(boolean unknownContentLength) throws Exception {
        byte[] body = bodyWithSize(AskRequestBodySizeFilter.MAX_REQUEST_BODY_BYTES + 1);
        MockHttpServletRequest request = request(body, unknownContentLength);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertPayloadTooLarge(response);
    }

    private MockHttpServletRequest request(byte[] body) {
        return request(body, false);
    }

    private MockHttpServletRequest request(byte[] body, boolean unknownContentLength) {
        MockHttpServletRequest request;
        if (unknownContentLength) {
            request = new MockHttpServletRequest("POST", "/ask") {
                @Override
                public long getContentLengthLong() {
                    return -1;
                }
            };
        } else {
            request = new MockHttpServletRequest("POST", "/ask");
        }
        request.setContentType("application/json");
        request.setContent(body);
        return request;
    }

    private byte[] bodyWithSize(int size) {
        String base = "{\"question\":\"" + "a".repeat(300) + "\"}";
        return (base + " ".repeat(Math.max(0, size - base.getBytes(StandardCharsets.UTF_8).length)))
                .getBytes(StandardCharsets.UTF_8);
    }

    private void assertPayloadTooLarge(MockHttpServletResponse response) throws Exception {
        assertEquals(413, response.getStatus());
        assertEquals(
                "{\"status\":413,\"error\":\"Payload Too Large\",\"message\":\"Request body must not exceed 16 KB\"}",
                response.getContentAsString(StandardCharsets.UTF_8)
        );
    }
}
