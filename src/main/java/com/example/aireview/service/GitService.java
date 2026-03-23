package com.example.aireview.service;

import com.example.aireview.config.GitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitService {

    private final GitProperties gitProperties;
    private final RestTemplate restTemplate;

    public void postComments(String repositoryName, String prId, List<String> comments) {
        log.info("Using Git API at {} to post comments on PR {} in repo {}", gitProperties.getUrl(), prId, repositoryName);
        
        String url = String.format("%s/repos/%s/issues/%s/comments", gitProperties.getUrl(), repositoryName, prId);
        HttpHeaders headers = createCommonHeaders();

        for (String comment : comments) {
            Map<String, String> body = Map.of("body", comment);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            
            try {
                restTemplate.postForObject(url, request, String.class);
                log.info("Successfully posted comment to PR.");
            } catch (Exception e) {
                log.error("Failed to post comment to GIT API: {}", e.getMessage());
            }
        }
    }

    public void approvePr(String repositoryName, String prId) {
        log.info("Approving PR {} in repo {}", prId, repositoryName);
        submitReviewLogic(repositoryName, prId, "APPROVE", "AI validation passed automatically.");
    }

    public void blockPr(String repositoryName, String prId) {
        log.info("Requesting changes / Blocking PR {} in repo {}", prId, repositoryName);
        submitReviewLogic(repositoryName, prId, "REQUEST_CHANGES", "AI Validation Failed. Changes are requested based on the review comments.");
    }

    public String getPrDiff(String repositoryName, String prId) {
        log.info("Fetching raw PR diff for repo {} PR {}", repositoryName, prId);
        String url = String.format("%s/repos/%s/pulls/%s", gitProperties.getUrl(), repositoryName, prId);
        HttpHeaders headers = createCommonHeaders();
        headers.set("Accept", "application/vnd.github.v3.diff"); // Tells GitHub to return the raw patch file
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, request, String.class).getBody();
        } catch (Exception e) {
            log.error("Failed to fetch PR diff: {}", e.getMessage());
            return "No Diff Found.";
        }
    }

    public void autoMergePr(String repositoryName, String prId) {
        log.info("Auto-merging PR {} in repo {}", prId, repositoryName);
        String url = String.format("%s/repos/%s/pulls/%s/merge", gitProperties.getUrl(), repositoryName, prId);
        HttpHeaders headers = createCommonHeaders();
        
        Map<String, String> body = Map.of(
            "commit_title", "Auto-merged by AI Review Agent",
            "merge_method", "squash"
        );
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.put(url, request); // merge is a PUT request
            log.info("Successfully merged PR {}.", prId);
        } catch (Exception e) {
            log.error("Failed to merge PR: {}", e.getMessage());
        }
    }

    private void submitReviewLogic(String repositoryName, String prId, String eventType, String fallbackBody) {
        String url = String.format("%s/repos/%s/pulls/%s/reviews", gitProperties.getUrl(), repositoryName, prId);
        HttpHeaders headers = createCommonHeaders();
        
        Map<String, String> body = Map.of(
            "body", fallbackBody,
            "event", eventType 
        );
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(url, request, String.class);
            log.info("Successfully submitted PR review with event: {}", eventType);
        } catch (Exception e) {
            log.error("Failed to submit review for PR: {}", e.getMessage());
        }
    }

    private HttpHeaders createCommonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gitProperties.getToken());
        // GitHub specific headers
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        headers.set("Accept", "application/vnd.github+json");
        return headers;
    }
}
