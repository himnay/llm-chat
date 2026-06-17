package com.org.llm.rag;

import com.org.llm.model.QueryTransformRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy-pattern dispatcher: picks the {@link QueryTransformationStrategy} matching the
 * request's {@link QueryTransformationTechnique} and delegates to it.
 */
@Service
public class QueryTransformationService {

    private final Map<QueryTransformationTechnique, QueryTransformationStrategy> strategiesByTechnique;

    public QueryTransformationService(List<QueryTransformationStrategy> strategies) {
        this.strategiesByTechnique = strategies.stream()
                .collect(Collectors.toMap(QueryTransformationStrategy::technique, Function.identity()));
    }

    public List<String> transform(QueryTransformRequest request) {
        QueryTransformationStrategy strategy = strategiesByTechnique.get(request.technique());
        if (strategy == null) {
            throw new IllegalStateException("No strategy registered for technique: " + request.technique());
        }
        return strategy.transform(request);
    }
}
