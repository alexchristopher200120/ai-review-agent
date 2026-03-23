package com.example.aireview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrEvent {
    private String repositoryName;
    private String branchName;
    private String prId;
    private String author;
    private String contentDiff;
}
