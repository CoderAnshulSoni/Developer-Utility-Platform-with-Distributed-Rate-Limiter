package com.anshul.devtoolkit.service;

import com.anshul.devtoolkit.model.SqlWarning;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SqlAnalyserServiceTest {

    private final SqlAnalyserService sqlAnalyserService = new SqlAnalyserService();

    @Test
    public void analyse_SelectStar_ShouldReturnHighWarning() {
        SqlAnalyserService.AnalysisResult result = sqlAnalyserService.analyse("SELECT * FROM users");
        
        assertNotNull(result);
        List<SqlWarning> warnings = result.getWarnings();
        assertNotNull(warnings);
        
        boolean foundSelectStar = warnings.stream()
                .anyMatch(w -> "SELECT_STAR".equals(w.getType()) && "HIGH".equals(w.getSeverity()));
        
        assertTrue(foundSelectStar, "Expected a SELECT_STAR warning with HIGH severity");
    }

    @Test
    public void analyse_DeleteWithoutWhere_ShouldReturnHighWarning() {
        SqlAnalyserService.AnalysisResult result = sqlAnalyserService.analyse("DELETE FROM orders");
        
        assertNotNull(result);
        List<SqlWarning> warnings = result.getWarnings();
        assertNotNull(warnings);
        
        boolean foundMissingWhere = warnings.stream()
                .anyMatch(w -> "MISSING_WHERE".equals(w.getType()) && "HIGH".equals(w.getSeverity()));
        
        assertTrue(foundMissingWhere, "Expected a MISSING_WHERE warning with HIGH severity for DELETE without WHERE");
    }

    @Test
    public void analyse_UpdateWithoutWhere_ShouldReturnHighWarning() {
        SqlAnalyserService.AnalysisResult result = sqlAnalyserService.analyse("UPDATE products SET price = 100");
        
        assertNotNull(result);
        List<SqlWarning> warnings = result.getWarnings();
        assertNotNull(warnings);
        
        boolean foundMissingWhere = warnings.stream()
                .anyMatch(w -> "MISSING_WHERE".equals(w.getType()) && "HIGH".equals(w.getSeverity()));
        
        assertTrue(foundMissingWhere, "Expected a MISSING_WHERE warning with HIGH severity for UPDATE without WHERE");
    }

    @Test
    public void analyse_SelectWithoutLimit_ShouldReturnMediumWarning() {
        SqlAnalyserService.AnalysisResult result = sqlAnalyserService.analyse("SELECT id, name FROM users WHERE active = true");
        
        assertNotNull(result);
        List<SqlWarning> warnings = result.getWarnings();
        assertNotNull(warnings);
        
        boolean foundNoLimit = warnings.stream()
                .anyMatch(w -> "NO_LIMIT".equals(w.getType()) && "MEDIUM".equals(w.getSeverity()));
        
        assertTrue(foundNoLimit, "Expected a NO_LIMIT warning with MEDIUM severity");
    }

    @Test
    public void analyse_GoodQuery_ShouldReturnHighScore() {
        SqlAnalyserService.AnalysisResult result = sqlAnalyserService.analyse("SELECT id, name FROM users WHERE active = true LIMIT 10");
        
        assertNotNull(result);
        assertTrue(result.getWarnings().isEmpty());
        assertEquals(100, result.getScore());
    }

    @Test
    public void analyse_MultipleIssues_ShouldDeductScoreCorrectly() {
        SqlAnalyserService.AnalysisResult result = sqlAnalyserService.analyse("SELECT * FROM users");
        
        assertNotNull(result);
        // Has SELECT_STAR (HIGH = -20) and NO_LIMIT (MEDIUM = -10). Total = 100 - 30 = 70.
        assertEquals(70, result.getScore());
    }

    @Test
    public void analyse_CartesianProduct_ShouldWarn() {
        // "SELECT * FROM users JOIN orders" has JOIN but not ON, which triggers CARTESIAN_PRODUCT
        SqlAnalyserService.AnalysisResult result = sqlAnalyserService.analyse("SELECT * FROM users JOIN orders");
        
        assertNotNull(result);
        List<SqlWarning> warnings = result.getWarnings();
        assertNotNull(warnings);
        
        boolean foundCartesian = warnings.stream()
                .anyMatch(w -> "CARTESIAN_PRODUCT".equals(w.getType()) && "HIGH".equals(w.getSeverity()));
        
        assertTrue(foundCartesian, "Expected a CARTESIAN_PRODUCT warning with HIGH severity");
    }

    @Test
    public void analyse_EmptyQuery_ShouldReturnScore100() {
        SqlAnalyserService.AnalysisResult result = sqlAnalyserService.analyse("");
        
        assertNotNull(result);
        assertTrue(result.getWarnings().isEmpty());
        assertEquals(100, result.getScore());
    }
}
