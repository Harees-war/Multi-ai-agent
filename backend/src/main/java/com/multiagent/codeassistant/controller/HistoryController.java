package com.multiagent.codeassistant.controller;

import com.multiagent.codeassistant.dto.HistoryResponse;
import com.multiagent.codeassistant.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<List<HistoryResponse>> getUserHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        List<HistoryResponse> historyList = historyService.getUserHistory(email);
        return ResponseEntity.ok(historyList);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteHistoryEntry(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        historyService.deleteHistoryEntry(email, id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "History entry deleted successfully");
        return ResponseEntity.ok(response);
    }
}
