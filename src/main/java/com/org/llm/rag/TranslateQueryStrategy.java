package com.org.llm.rag;

import com.org.llm.model.QueryTransformRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

/** Translates the query into the language of the indexed documents. */
@Component
@RequiredArgsConstructor
class TranslateQueryStrategy implements QueryTransformationStrategy {

    private static final String DEFAULT_TARGET_LANGUAGE = "English";

    private final ChatClient.Builder ragChatClientBuilder;

    @Override
    public QueryTransformationTechnique technique() {
        return QueryTransformationTechnique.TRANSLATE;
    }

    @Override
    public List<String> transform(QueryTransformRequest request) {
        // Target language varies per request, so the transformer is built here rather than as a
        // fixed singleton bean — building it is cheap, no LLM call happens until .transform() runs.
        TranslationQueryTransformer translationQueryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(ragChatClientBuilder)
                .targetLanguage(request.targetLanguage() != null ? request.targetLanguage() : DEFAULT_TARGET_LANGUAGE)
                .build();
        Query translated = translationQueryTransformer.transform(new Query(request.query()));
        return List.of(translated.text());
    }
}
