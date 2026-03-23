package com.example.aireview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "git.api")
@Data
public class GitProperties {
    private String token;
    private String url;
}
