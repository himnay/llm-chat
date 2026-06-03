package com.org.llm.model;

import java.util.List;
import java.util.Map;

public record TextToSqlResponse(
        String question,
        String sql,
        boolean repaired,
        boolean dryRun,
        int rowCount,
        List<Map<String, Object>> rows,
        String explanation
) {
}
