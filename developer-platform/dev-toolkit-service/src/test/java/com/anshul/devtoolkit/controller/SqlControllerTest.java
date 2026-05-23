package com.anshul.devtoolkit.controller;

import com.anshul.devtoolkit.service.ExplainService;
import com.anshul.devtoolkit.service.ExplainService.ExplainResult;
import com.anshul.devtoolkit.service.SqlAnalyserService;
import com.anshul.devtoolkit.service.SqlAnalyserService.AnalysisResult;
import com.anshul.devtoolkit.model.SqlWarning;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SqlController.class)
@ContextConfiguration(classes = SqlControllerTest.TestConfig.class)
public class SqlControllerTest {

    @SpringBootConfiguration
    @org.springframework.context.annotation.Import(SqlController.class)
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SqlAnalyserService sqlAnalyserService;

    @MockBean
    private ExplainService explainService;

    @Test
    public void analyseEndpoint_WithValidSql_ShouldReturn200() throws Exception {
        AnalysisResult analysisResult = AnalysisResult.builder()
                .originalQuery("SELECT id FROM users")
                .warnings(new ArrayList<>())
                .score(80)
                .build();

        when(sqlAnalyserService.analyse(eq("SELECT id FROM users"))).thenReturn(analysisResult);

        mockMvc.perform(post("/api/sql/analyse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT id FROM users\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(80));
    }

    @Test
    public void analyseEndpoint_WithEmptyBody_ShouldReturn400() throws Exception {
        // Test empty body '{}' which resolves to SqlRequest with null/empty sql field
        mockMvc.perform(post("/api/sql/analyse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void analyseEndpoint_ShouldReturnWarnings() throws Exception {
        SqlWarning w1 = SqlWarning.builder()
                .type("SELECT_STAR")
                .severity("HIGH")
                .message("Avoid SELECT *")
                .suggestion("Specify columns")
                .build();
        SqlWarning w2 = SqlWarning.builder()
                .type("NO_LIMIT")
                .severity("MEDIUM")
                .message("Missing LIMIT")
                .suggestion("Add LIMIT")
                .build();

        AnalysisResult analysisResult = AnalysisResult.builder()
                .originalQuery("SELECT * FROM users")
                .warnings(Arrays.asList(w1, w2))
                .score(70)
                .build();

        when(sqlAnalyserService.analyse(eq("SELECT * FROM users"))).thenReturn(analysisResult);

        mockMvc.perform(post("/api/sql/analyse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT * FROM users\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings", hasSize(2)))
                .andExpect(jsonPath("$.warnings[0].type").value("SELECT_STAR"))
                .andExpect(jsonPath("$.warnings[1].type").value("NO_LIMIT"));
    }

    @Test
    public void explainEndpoint_WithValidSql_ShouldReturn200() throws Exception {
        ExplainResult explainResult = ExplainResult.builder()
                .rawPlan("Seq Scan on users")
                .nodes(new ArrayList<>())
                .bottlenecks(new ArrayList<>())
                .build();

        when(explainService.explainQuery(eq("SELECT * FROM users"))).thenReturn(explainResult);

        mockMvc.perform(post("/api/sql/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT * FROM users\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawPlan").value("Seq Scan on users"));
    }

    @Test
    public void explainEndpoint_WhenServiceThrows_ShouldReturn500() throws Exception {
        when(explainService.explainQuery(anyString())).thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(post("/api/sql/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sql\":\"SELECT * FROM users\"}"))
                .andExpect(status().isInternalServerError());
    }
}
