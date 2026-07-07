package com.multiagent.codeassistant.dto;

import jakarta.validation.constraints.NotBlank;

public class AiRequest {
    private String language;

    @NotBlank(message = "Prompt or code content is required")
    private String prompt;

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
}
