package com.org.llm.rag;

import com.org.llm.model.QueryTransformRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Compresses conversation history plus the current query into a single standalone query. */
@Component
@RequiredArgsConstructor
class CompressQueryStrategy implements QueryTransformationStrategy {

    private final CompressionQueryTransformer compressionQueryTransformer;

    @Override
    public QueryTransformationTechnique technique() {
        return QueryTransformationTechnique.COMPRESS;
    }

    @Override
    public List<String> transform(QueryTransformRequest request) {
        Query query = Query.builder()
                .text(request.query())
                .history(toHistory(request.history()))
                .build();
        Query compressed = compressionQueryTransformer.transform(query);
        return List.of(compressed.text());
    }

    // Alternating turns, oldest first, starting with the user — see QueryTransformRequest#history
    private static List<Message> toHistory(List<String> turns) {
        List<Message> history = new ArrayList<>();
        if (turns == null) {
            return history;
        }
        for (int i = 0; i < turns.size(); i++) {
            history.add(i % 2 == 0 ? new UserMessage(turns.get(i)) : new AssistantMessage(turns.get(i)));
        }
        return history;
    }
}
