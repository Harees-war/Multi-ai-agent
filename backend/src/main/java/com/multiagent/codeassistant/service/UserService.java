package com.multiagent.codeassistant.service;

import com.multiagent.codeassistant.dto.ProfileResponse;
import com.multiagent.codeassistant.dto.ProfileUpdateRequest;
import com.multiagent.codeassistant.exception.BadRequestException;
import com.multiagent.codeassistant.exception.ResourceNotFoundException;
import com.multiagent.codeassistant.model.User;
import com.multiagent.codeassistant.repository.HistoryRepository;
import com.multiagent.codeassistant.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, HistoryRepository historyRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());

        response.setTotalRequests(historyRepository.countByUserId(user.getId()));
        response.setCodeGenerations(historyRepository.countByUserIdAndAgent(user.getId(), "Generator"));
        response.setCodeReviews(historyRepository.countByUserIdAndAgent(user.getId(), "Reviewer"));
        response.setCodeExplanations(historyRepository.countByUserIdAndAgent(user.getId(), "Explainer"));

        return response;
    }

    @Transactional
    public ProfileResponse updateUserProfile(String email, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        user.setName(request.getName());

        if (StringUtils.hasText(request.getNewPassword())) {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new BadRequestException("Current password is required to set a new password");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BadRequestException("Current password does not match!");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);

        return getUserProfile(email);
    }
}
