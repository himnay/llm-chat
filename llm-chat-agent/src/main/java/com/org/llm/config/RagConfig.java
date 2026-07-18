package com.org.llm.config;

import com.org.llm.rag.RagFilterContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
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

    /** Defines the rag chat client builder bean. */
    @Bean
    public ChatClient.Builder ragChatClientBuilder(OpenAiChatModel openAiChatModel) {
        // Low temperature keeps query transformation/expansion deterministic (Spring AI RAG
        // guidance); kept separate from the main conversational ChatClient in AIConfig so it never
        // affects answer generation.
        return ChatClient.builder(openAiChatModel)
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.0));
    }

    /** Defines the compression query transformer bean. */
    @Bean
    public CompressionQueryTransformer compressionQueryTransformer(ChatClient.Builder ragChatClientBuilder) {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(ragChatClientBuilder)
                .build();
    }

    /** Defines the rewrite query transformer bean. */
    @Bean
    public RewriteQueryTransformer rewriteQueryTransformer(ChatClient.Builder ragChatClientBuilder) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ragChatClientBuilder)
                .targetSearchSystem("a vector store holding corporate travel-policy and events documents")
                .build();
    }

    /** Defines the multi query expander bean. */
    @Bean
    public MultiQueryExpander multiQueryExpander(ChatClient.Builder ragChatClientBuilder) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(ragChatClientBuilder)
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();
    }

    /** Defines the retrieval augmentation advisor bean. */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore,
                                                                      CompressionQueryTransformer compressionQueryTransformer,
                                                                      MultiQueryExpander multiQueryExpander,
                                                                      RagFilterContext ragFilterContext) {
        // Modular RAG pipeline: compress (history + follow-up) into a standalone query, expand it
        // into several paraphrased variants (MultiQueryExpander), retrieve each variant from the
        // Redis vector store, merge/dedup with ConcatenationDocumentJoiner, then augment the
        // prompt — so "what about the second one?" resolves against prior turns instead of being
        // embedded and searched verbatim, and phrasing-sensitive recall misses are reduced.
        // ragFilterContext::get lets LocalChatBackend scope retrieval to one document per request
        // (e.g. fileName) without rebuilding this singleton advisor on every call.
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(compressionQueryTransformer)
                .queryExpander(multiQueryExpander)
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .filterExpression(ragFilterContext::get)
                        .build())
                .documentJoiner(new ConcatenationDocumentJoiner())
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
                .build();
    }
}
