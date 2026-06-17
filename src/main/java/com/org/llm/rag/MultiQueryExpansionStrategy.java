package com.org.llm.rag;

import com.org.llm.model.QueryTransformRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Component;

import java.util.List;

/** Generates several paraphrased variants of the query to improve recall on ambiguous questions. */
@Component
@RequiredArgsConstructor
class MultiQueryExpansionStrategy implements QueryTransformationStrategy {

    private final MultiQueryExpander multiQueryExpander;

    @Override
    public QueryTransformationTechnique technique() {
        return QueryTransformationTechnique.MULTI_QUERY_EXPAND;
    }

    @Override
    public List<String> transform(QueryTransformRequest request) {
        return multiQueryExpander.expand(new Query(request.query())).stream()
                .map(Query::text)
                .toList();
    }
}
