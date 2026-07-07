package com.multiagent.codeassistant.controller;

import com.multiagent.codeassistant.dto.AiRequest;
import com.multiagent.codeassistant.dto.AiResponse;
import com.multiagent.codeassistant.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate")
    public ResponseEntity<AiResponse> generateCode(@Valid @RequestBody AiRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String result = aiService.generateCode(email, request.getLanguage(), request.getPrompt());
        return ResponseEntity.ok(new AiResponse(result));
    }

    @PostMapping("/review")
    public ResponseEntity<AiResponse> reviewCode(@Valid @RequestBody AiRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String result = aiService.reviewCode(email, request.getLanguage(), request.getPrompt());
        return ResponseEntity.ok(new AiResponse(result));
    }

    @PostMapping("/explain")
    public ResponseEntity<AiResponse> explainCode(@Valid @RequestBody AiRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String result = aiService.explainCode(email, request.getLanguage(), request.getPrompt());
        return ResponseEntity.ok(new AiResponse(result));
    }
}
