package com.example.aireview.service;

import com.example.aireview.model.PrEvent;
import com.example.aireview.model.ReviewResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewAgentService {

    private final ChatGptService chatGptService;
    private final GitService gitService;

    public void processPrEvent(PrEvent event) {
        log.info("Starting AI Review for PR {} in repo {}", event.getPrId(), event.getRepositoryName());
        
        ReviewResult result = conductAiReview(event.getContentDiff());
        
        // 1. ALWAYS post the detailed AI Review changes as a PR comment
        String masterComment = buildMasterComment(result);
        gitService.postComments(event.getRepositoryName(), event.getPrId(), List.of(masterComment));

        // 2. Decide the Gate Evaluation
        if (result.isApproved()) {
            log.info("PR {} passed all checks.", event.getPrId());
            gitService.approvePr(event.getRepositoryName(), event.getPrId());
            gitService.autoMergePr(event.getRepositoryName(), event.getPrId());
        } else {
            log.warn("PR {} failed checks. Blocking PR.", event.getPrId());
            gitService.blockPr(event.getRepositoryName(), event.getPrId());
        }
    }

    private String buildMasterComment(ReviewResult result) {
        StringBuilder sb = new StringBuilder("## 🤖 AI Pull Request Review\n\n");
        sb.append(result.isApproved() ? "✅ **OVERALL VERDICT: PASSED**\n\n" : "❌ **OVERALL VERDICT: CHANGES REQUESTED**\n\n");
        sb.append("Here is the detailed feedback across all analyzed dimensions:\n\n");
        for (String comment : result.getComments()) {
            sb.append(comment).append("\n\n---\n");
        }
        return sb.toString();
    }

    private ReviewResult conductAiReview(String diff) {
        CompletableFuture<String> codeStandardsFuture = CompletableFuture.supplyAsync(
                () -> chatGptService.analyzeCodeDiff(diff, "Code standards (naming, formatting, style guide)")
        );
        CompletableFuture<String> securityScanFuture = CompletableFuture.supplyAsync(
                () -> chatGptService.analyzeCodeDiff(diff, "Security scan (SAST, secrets, CVEs)")
        );
        CompletableFuture<String> vulnerabilityCheckFuture = CompletableFuture.supplyAsync(
                () -> chatGptService.analyzeCodeDiff(diff, "Vulnerability check (deps, OWASP risks)")
        );
        CompletableFuture<String> performanceFuture = CompletableFuture.supplyAsync(
                () -> chatGptService.analyzeCodeDiff(diff, "Performance (complexity, anti-patterns)")
        );
        CompletableFuture<String> unitTestValidationFuture = CompletableFuture.supplyAsync(
                () -> chatGptService.analyzeCodeDiff(diff, "Unit test validation (coverage, test quality)")
        );

        CompletableFuture.allOf(
                codeStandardsFuture, securityScanFuture, vulnerabilityCheckFuture, performanceFuture, unitTestValidationFuture
        ).join();

        boolean allPassed = true;
        List<String> combinedComments = new ArrayList<>();

        if (!evaluateDimension("Code Standards", codeStandardsFuture.join(), combinedComments)) allPassed = false;
        if (!evaluateDimension("Security Scan", securityScanFuture.join(), combinedComments)) allPassed = false;
        if (!evaluateDimension("Vulnerabilities", vulnerabilityCheckFuture.join(), combinedComments)) allPassed = false;
        if (!evaluateDimension("Performance", performanceFuture.join(), combinedComments)) allPassed = false;
        if (!evaluateDimension("Unit Tests", unitTestValidationFuture.join(), combinedComments)) allPassed = false;

        return ReviewResult.builder()
                .isApproved(allPassed)
                .comments(combinedComments)
                .build();
    }

    private boolean evaluateDimension(String dimensionName, String aiFeedback, List<String> comments) {
        boolean passed = !aiFeedback.startsWith("FAILED:");
        String icon = passed ? "✅" : "❌";
        
        String cleanFeedback = aiFeedback.replaceFirst("^(PASSED:|FAILED:)\\s*", "").trim();
        comments.add(String.format("### %s %s\n%s", icon, dimensionName, cleanFeedback));
        
        return passed;
    }
}
