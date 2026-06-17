package com.org.llm.rag;

import com.org.llm.model.QueryTransformRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

/** LLM rewrites a messy/conversational query into a clean, standalone search query. */
@Component
@RequiredArgsConstructor
class RewriteQueryStrategy implements QueryTransformationStrategy {

    private final RewriteQueryTransformer rewriteQueryTransformer;

    @Override
    public QueryTransformationTechnique technique() {
        return QueryTransformationTechnique.REWRITE;
    }

    @Override
    public List<String> transform(QueryTransformRequest request) {
        Query rewritten = rewriteQueryTransformer.transform(new Query(request.query()));
        return List.of(rewritten.text());
    }
}
