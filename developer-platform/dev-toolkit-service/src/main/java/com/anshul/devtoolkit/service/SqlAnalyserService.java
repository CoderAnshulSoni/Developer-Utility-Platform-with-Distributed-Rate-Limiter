package com.anshul.devtoolkit.service;

import com.anshul.devtoolkit.model.SqlWarning;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SqlAnalyserService {

    public AnalysisResult analyse(String sql) {
        if (sql == null) {
            sql = "";
        }

        List<SqlWarning> warnings = new ArrayList<>();

        if (checkSelectStar(sql)) {
            warnings.add(SqlWarning.builder()
                    .type("SELECT_STAR")
                    .severity("HIGH")
                    .message("Avoid using SELECT * in queries.")
                    .suggestion("Specify explicit column names")
                    .build());
        }

        if (checkMissingWhere(sql)) {
            warnings.add(SqlWarning.builder()
                    .type("MISSING_WHERE")
                    .severity("HIGH")
                    .message("UPDATE or DELETE query is missing a WHERE clause.")
                    .suggestion("Add WHERE clause to prevent full table update/delete")
                    .build());
        }

        if (checkNoLimit(sql)) {
            warnings.add(SqlWarning.builder()
                    .type("NO_LIMIT")
                    .severity("MEDIUM")
                    .message("SELECT query is missing a LIMIT or ROWNUM clause.")
                    .suggestion("Add LIMIT to prevent large result sets")
                    .build());
        }

        if (checkMultipleJoins(sql)) {
            warnings.add(SqlWarning.builder()
                    .type("MULTIPLE_JOINS")
                    .severity("MEDIUM")
                    .message("Query contains more than 3 JOIN operations.")
                    .suggestion("Consider breaking into smaller queries or using CTEs")
                    .build());
        }

        if (checkCartesianProduct(sql)) {
            warnings.add(SqlWarning.builder()
                    .type("CARTESIAN_PRODUCT")
                    .severity("HIGH")
                    .message("Query has a JOIN without an ON clause.")
                    .suggestion("Add ON condition to avoid cartesian product")
                    .build());
        }

        // Scoring
        int score = 100;
        for (SqlWarning warning : warnings) {
            if ("HIGH".equalsIgnoreCase(warning.getSeverity())) {
                score -= 20;
            } else if ("MEDIUM".equalsIgnoreCase(warning.getSeverity())) {
                score -= 10;
            } else if ("LOW".equalsIgnoreCase(warning.getSeverity())) {
                score -= 5;
            }
        }
        score = Math.max(0, score);

        return AnalysisResult.builder()
                .originalQuery(sql)
                .warnings(warnings)
                .score(score)
                .build();
    }

    private boolean checkSelectStar(String sql) {
        return sql.toUpperCase().matches("(?s).*SELECT\\s+\\*.*");
    }

    private boolean checkMissingWhere(String sql) {
        String upper = sql.toUpperCase();
        boolean isUpdateOrDelete = upper.contains("UPDATE") || upper.contains("DELETE");
        return isUpdateOrDelete && !upper.contains("WHERE");
    }

    private boolean checkNoLimit(String sql) {
        String upper = sql.toUpperCase();
        return upper.contains("SELECT") && !upper.contains("LIMIT") && !upper.contains("ROWNUM");
    }

    private boolean checkMultipleJoins(String sql) {
        String upper = sql.toUpperCase();
        String[] parts = upper.split("\\bJOIN\\b");
        return (parts.length - 1) > 3;
    }

    private boolean checkCartesianProduct(String sql) {
        String upper = sql.toUpperCase();
        return upper.contains("JOIN") && !upper.contains("ON");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResult {
        private String originalQuery;
        private List<SqlWarning> warnings;
        private int score;
    }
}
