package com.example.aireview.service;

import com.example.aireview.config.ChatGptProperties;
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
public class ChatGptService {

    private final ChatGptProperties chatGptProperties;
    private final RestTemplate restTemplate;

    /**
     * Calls OpenAI API to evaluate a specific dimension.
     */
    public String analyzeCodeDiff(String diff, String dimensionPrompt) {
        log.info("Calling ChatGPT API at {} for dimension: {}", chatGptProperties.getUrl(), dimensionPrompt);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(chatGptProperties.getKey());

        String systemMessage = "You are an expert AI code reviewer. Evaluate the diff strictly prioritizing this dimension: " + dimensionPrompt + ". "
                + "If the code passes, start your response exactly with 'PASSED: '. "
                + "If there are issues, start your response exactly with 'FAILED: '. "
                + "Always provide constructive, markdown-formatted feedback after the prefix.";
                
        String userMessage = "Code Diff:\n" + diff;

        Map<String, Object> requestBody = Map.of(
            "model", chatGptProperties.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
            ),
            "temperature", 0.2
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map response = restTemplate.postForObject(chatGptProperties.getUrl(), request, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, String> message = (Map<String, String>) choices.get(0).get("message");
                    return message.get("content");
                }
            }
            return "FAILED: AI produced an empty or unstructured response.";
        } catch (Exception e) {
            log.error("API error for dimension {}: {}", dimensionPrompt, e.getMessage());
            return "FAILED: External API exception occurred: " + e.getMessage();
        }
    }
}
