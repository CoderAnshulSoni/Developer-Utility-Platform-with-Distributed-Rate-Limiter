package com.anshul.devtoolkit.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExplainService {

    private final DataSource dataSource;

    public ExplainService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ExplainResult explainQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return ExplainResult.builder()
                    .rawPlan("")
                    .nodes(new ArrayList<>())
                    .bottlenecks(new ArrayList<>())
                    .build();
        }

        String explainSql = "EXPLAIN ANALYZE " + sql;
        StringBuilder sb = new StringBuilder();
        List<String> lines = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {

            while (rs.next()) {
                String line = rs.getString(1);
                lines.add(line);
                sb.append(line).append("\n");
            }

        } catch (Exception e) {
            String errorMsg = "Error executing EXPLAIN ANALYZE: " + e.getMessage();
            List<String> errorBottlenecks = new ArrayList<>();
            errorBottlenecks.add("Failed to explain query: " + e.getMessage());
            return ExplainResult.builder()
                    .rawPlan(errorMsg)
                    .nodes(new ArrayList<>())
                    .bottlenecks(errorBottlenecks)
                    .build();
        }

        String rawPlan = sb.toString();
        List<ExplainNode> nodes = new ArrayList<>();
        List<String> bottlenecks = new ArrayList<>();

        Pattern costPattern = Pattern.compile("cost=\\d+\\.\\d+\\.\\.(\\d+\\.\\d+)");
        Pattern rowsPattern = Pattern.compile("rows=(\\d+)");

        for (String line : lines) {
            String upper = line.toUpperCase();
            if (upper.contains("COST=") && upper.contains("ROWS=")) {
                double cost = 0.0;
                int rows = 0;

                Matcher costMatcher = costPattern.matcher(line);
                if (costMatcher.find()) {
                    cost = Double.parseDouble(costMatcher.group(1));
                }

                Matcher rowsMatcher = rowsPattern.matcher(line);
                if (rowsMatcher.find()) {
                    rows = Integer.parseInt(rowsMatcher.group(1));
                }

                String nodeType = "";
                if (line.contains("->")) {
                    String[] parts = line.split("->");
                    if (parts[0].trim().matches(".*[a-zA-Z].*")) {
                        nodeType = parts[0].trim();
                    } else if (parts.length > 1) {
                        String after = parts[1].trim();
                        int parenIndex = after.indexOf('(');
                        if (parenIndex != -1) {
                            nodeType = after.substring(0, parenIndex).trim();
                        } else {
                            nodeType = after;
                        }
                    }
                } else {
                    int parenIndex = line.indexOf('(');
                    if (parenIndex != -1) {
                        nodeType = line.substring(0, parenIndex).trim();
                    } else {
                        nodeType = line.trim();
                    }
                }

                boolean isBottleneck = cost > 1000.0;
                if (isBottleneck) {
                    bottlenecks.add("High cost node detected: " + nodeType + " (cost=" + cost + ")");
                }

                nodes.add(ExplainNode.builder()
                        .nodeType(nodeType)
                        .cost(cost)
                        .rows(rows)
                        .details(line.trim())
                        .isBottleneck(isBottleneck)
                        .build());
            }
        }

        return ExplainResult.builder()
                .rawPlan(rawPlan)
                .nodes(nodes)
                .bottlenecks(bottlenecks)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExplainResult {
        private String rawPlan;
        private List<ExplainNode> nodes;
        private List<String> bottlenecks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExplainNode {
        private String nodeType;
        private double cost;
        private int rows;
        private String details;
        private boolean isBottleneck;
    }
}
