package com.example.aireview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResult {
    private boolean isApproved;
    private List<String> comments;
    
    // Detailed dimension results
    private DimensionResult codeStandards;
    private DimensionResult securityScan;
    private DimensionResult vulnerabilityCheck;
    private DimensionResult performance;
    private DimensionResult unitTestValidation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DimensionResult {
        private boolean passed;
        private String feedback;
    }
}
