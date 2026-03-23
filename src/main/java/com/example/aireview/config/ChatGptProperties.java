package com.example.aireview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "chatgpt.api")
@Data
public class ChatGptProperties {
    private String key;
    private String url;
    private String model;
}
