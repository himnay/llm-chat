package com.org.llm;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.ai.vectorstore.redis.enabled=false",
        "app.weather.api-key=test-key",
        "spring.ai.openai.api-key=test-key",
        "spring.ai.stabilityai.api-key=test-key"
})
@Import(TestcontainersConfiguration.class)
class LLMApplicationTests {

    @MockitoBean
    private VectorStore vectorStore;

    @Test
    void contextLoads() {
    }
}
