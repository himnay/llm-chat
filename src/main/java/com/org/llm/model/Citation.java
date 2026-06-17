package com.org.llm.model;

import org.springframework.ai.document.Document;

import java.util.Map;

/**
 * A single retrieved-document reference backing a RAG answer, surfaced to the client so it can
 * show "answered using X" provenance.
 *
 * <p>Fields mirror the metadata keys actually written by {@code llm-rag-pipeline}'s ingestion
 * pipeline into the shared Redis vector store (see {@code DocumentReaderFactory} and
 * {@code ChunkVectorStoreService} in that project): every chunk carries {@code fileName},
 * {@code source}, {@code identity} and {@code chunkIndex}; PDF-derived chunks (this app's corpus —
 * travel-policy and events PDFs, see {@code RagConfig}) additionally carry a page number under
 * Spring AI's PDF reader key {@code page_number} (older versions used {@code page}), which is why
 * {@code page} is nullable here — non-PDF sources won't have it.</p>
 */
public record Citation(
        String fileName,
        String source,
        String identity,
        Integer chunkIndex,
        Integer page
) {

    public static Citation from(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new Citation(
                asString(metadata.get("fileName")),
                asString(metadata.get("source")),
                asString(metadata.get("identity")),
                asInteger(metadata.get("chunkIndex")),
                asInteger(metadata.getOrDefault("page_number", metadata.get("page")))
        );
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
