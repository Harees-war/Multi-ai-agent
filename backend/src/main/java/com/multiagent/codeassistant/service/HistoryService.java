package com.multiagent.codeassistant.service;

import com.multiagent.codeassistant.dto.HistoryResponse;
import com.multiagent.codeassistant.exception.BadRequestException;
import com.multiagent.codeassistant.exception.ResourceNotFoundException;
import com.multiagent.codeassistant.model.History;
import com.multiagent.codeassistant.model.User;
import com.multiagent.codeassistant.repository.HistoryRepository;
import com.multiagent.codeassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final UserRepository userRepository;

    public HistoryService(HistoryRepository historyRepository, UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getUserHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return historyRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(h -> new HistoryResponse(
                        h.getId(),
                        h.getAgent(),
                        h.getLanguage(),
                        h.getPrompt(),
                        h.getResponse(),
                        h.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteHistoryEntry(String email, Long historyId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        History history = historyRepository.findById(historyId)
                .orElseThrow(() -> new ResourceNotFoundException("History entry not found"));

        if (!history.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have permission to delete this entry");
        }

        historyRepository.delete(history);
    }
}
