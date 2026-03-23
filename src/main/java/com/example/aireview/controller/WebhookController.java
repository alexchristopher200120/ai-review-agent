package com.example.aireview.controller;

import com.example.aireview.model.PrEvent;
import com.example.aireview.service.ReviewAgentService;
import com.example.aireview.service.GitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ReviewAgentService reviewAgentService;
    private final GitService gitService;

    @PostMapping("/pr-event")
    public ResponseEntity<String> handlePrEvent(@RequestBody PrEvent prEvent) {
        log.info("Received PR Event for repo: {}, PR ID: {}", prEvent.getRepositoryName(), prEvent.getPrId());
        
        // Auto-fetch the diff directly from GitHub if it's not supplied in the payload!
        if (prEvent.getContentDiff() == null || prEvent.getContentDiff().isBlank()) {
            String diff = gitService.getPrDiff(prEvent.getRepositoryName(), prEvent.getPrId());
            prEvent.setContentDiff(diff);
            log.info("Auto-fetched diff of {} characters.", diff.length());
        }

        // Process asynchronously
        new Thread(() -> reviewAgentService.processPrEvent(prEvent)).start();
        
        return ResponseEntity.accepted().body("PR Event received and review initiated.");
    }
}
