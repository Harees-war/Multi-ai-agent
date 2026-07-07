package com.multiagent.codeassistant.dto;

import java.time.LocalDateTime;

public class ProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private long totalRequests;
    private long codeGenerations;
    private long codeReviews;
    private long codeExplanations;

    public ProfileResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getCodeGenerations() {
        return codeGenerations;
    }

    public void setCodeGenerations(long codeGenerations) {
        this.codeGenerations = codeGenerations;
    }

    public long getCodeReviews() {
        return codeReviews;
    }

    public void setCodeReviews(long codeReviews) {
        this.codeReviews = codeReviews;
    }

    public long getCodeExplanations() {
        return codeExplanations;
    }

    public void setCodeExplanations(long codeExplanations) {
        this.codeExplanations = codeExplanations;
    }
}
