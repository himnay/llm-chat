package com.org.llm.rag;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

/**
 * Carries the current request's vector-store filter so the singleton
 * {@code VectorStoreDocumentRetriever} bean (see {@code RagConfig}) can apply a per-request
 * {@link Filter.Expression} via its {@code filterExpression(Supplier)} hook without being rebuilt
 * on every call. {@code LocalChatBackend} sets the value before invoking the {@code ChatClient}
 * and clears it in a {@code finally} block.
 */
@Component
public class RagFilterContext {

    private final ThreadLocal<Filter.Expression> expression = new ThreadLocal<>();

    /** Handles set. */
    public void set(Filter.Expression value) {
        expression.set(value);
    }

    /** Returns the get. */
    public Filter.Expression get() {
        return expression.get();
    }

    /** Clears. */
    public void clear() {
        expression.remove();
    }
}
