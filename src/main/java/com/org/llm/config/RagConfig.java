package com.org.llm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans for Spring AI's modular RAG (Advisors) API: the pre-retrieval query transformers/expander
 * and the {@link RetrievalAugmentationAdvisor} that wires the production retrieve → filter →
 * augment → generate pipeline used by {@code LocalChatBackend.chat()}/{@code stream()}.
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
class RagConfig {

    @Bean
    public ChatClient.Builder ragChatClientBuilder(OpenAiChatModel openAiChatModel) {
        // Low temperature keeps query transformation/expansion deterministic (Spring AI RAG
        // guidance); kept separate from the main conversational ChatClient in AIConfig so it never
        // affects answer generation.
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.0));
    }

    @Bean
    public CompressionQueryTransformer compressionQueryTransformer(ChatClient.Builder ragChatClientBuilder) {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(ragChatClientBuilder)
                .build();
    }

    @Bean
    public RewriteQueryTransformer rewriteQueryTransformer(ChatClient.Builder ragChatClientBuilder) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ragChatClientBuilder)
                .targetSearchSystem("a vector store holding corporate travel-policy and events documents")
                .build();
    }

    @Bean
    public MultiQueryExpander multiQueryExpander(ChatClient.Builder ragChatClientBuilder) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(ragChatClientBuilder)
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore,
                                                                      CompressionQueryTransformer compressionQueryTransformer) {
        // Modular RAG pipeline: compress (history + follow-up) into a standalone query, retrieve
        // from the Redis vector store, then augment the prompt — so "what about the second one?"
        // resolves against prior turns instead of being embedded and searched verbatim.
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(compressionQueryTransformer)
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
                .build();
    }
}
