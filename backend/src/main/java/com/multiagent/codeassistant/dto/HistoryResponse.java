package com.multiagent.codeassistant.dto;

import java.time.LocalDateTime;

public class HistoryResponse {
    private Long id;
    private String agent;
    private String language;
    private String prompt;
    private String response;
    private LocalDateTime createdAt;

    public HistoryResponse() {
    }

    public HistoryResponse(Long id, String agent, String language, String prompt, String response, LocalDateTime createdAt) {
        this.id = id;
        this.agent = agent;
        this.language = language;
        this.prompt = prompt;
        this.response = response;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
