package com.multiagent.codeassistant.service;

import com.multiagent.codeassistant.model.History;
import com.multiagent.codeassistant.model.User;
import com.multiagent.codeassistant.repository.HistoryRepository;
import com.multiagent.codeassistant.repository.UserRepository;
import com.multiagent.codeassistant.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;
    private final RestTemplate restTemplate;

    public AiService(UserRepository userRepository, HistoryRepository historyRepository) {
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.restTemplate = new RestTemplate();
    }

    public String generateCode(String email, String language, String requirement) {
        String systemInstruction = "You are a professional software engineer. Generate clean, modular, and optimized code. " +
                "Language: " + language + ". Requirement: " + requirement + ". Return only the code inside Markdown code blocks. Do not add explanations or surrounding discussion.";
        
        String response = callGemini(systemInstruction);
        saveHistory(email, "Generator", language, requirement, response);
        return response;
    }

    public String reviewCode(String email, String language, String code) {
        String systemInstruction = "You are a senior code reviewer. Review this " + language + " code:\n\n" + code + "\n\n" +
                "Return a detailed report in Markdown format with the following headings:\n" +
                "### Bug Detection\n" +
                "### Security Issues\n" +
                "### Performance Suggestions\n" +
                "### Best Practices & Code Smells\n" +
                "### Optimized Code\n" +
                "(Include the optimized code inside Markdown blocks)";

        String response = callGemini(systemInstruction);
        saveHistory(email, "Reviewer", language, code.length() > 100 ? code.substring(0, 100) + "..." : code, response);
        return response;
    }

    public String explainCode(String email, String language, String code) {
        String systemInstruction = "You are an expert programming tutor. Explain this " + language + " code:\n\n" + code + "\n\n" +
                "Provide a detailed report in Markdown format with the following headings:\n" +
                "### Line-by-Line Explanation\n" +
                "### Flow and Algorithms\n" +
                "### Complexity Analysis\n" +
                "- Time Complexity:\n" +
                "- Space Complexity:\n" +
                "### Real-World Example\n" +
                "### Summary";

        String response = callGemini(systemInstruction);
        saveHistory(email, "Explainer", language, code.length() > 100 ? code.substring(0, 100) + "..." : code, response);
        return response;
    }

    private void saveHistory(String email, String agent, String language, String prompt, String response) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        History history = new History(user, agent, language, prompt, response);
        historyRepository.save(history);
    }

    private String callGemini(String promptText) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("${")) {
            logger.warn("GEMINI_API_KEY is not configured. Returning fallback mock response.");
            return getMockResponse(promptText);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", promptText);
            
            Map<String, Object> partContainer = new HashMap<>();
            partContainer.put("parts", Collections.singletonList(textPart));
            
            requestBody.put("contents", Collections.singletonList(partContainer));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String url = apiUrl + "?key=" + apiKey;

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, request, Map.class);
            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                Map body = responseEntity.getBody();
                List candidates = (List) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map content = (Map) firstCandidate.get("content");
                    if (content != null) {
                        List parts = (List) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map firstPart = (Map) parts.get(0);
                            return (String) firstPart.get("text");
                        }
                    }
                }
            }
            throw new RuntimeException("Unexpected response format from Gemini API");
        } catch (Exception e) {
            logger.error("Error calling Gemini API: " + e.getMessage(), e);
            return "### Error calling Gemini API\n" +
                    "An error occurred: " + e.getMessage() + "\n\n" +
                    "#### Here is a simulated response:\n" +
                    getMockResponse(promptText);
        }
    }

    private String getMockResponse(String promptText) {
        if (promptText.contains("senior code reviewer")) {
            return "### Bug Detection\n" +
                    "1. **Null Pointer Exception**: The input code does not check if the parameter is null before accessing properties.\n" +
                    "2. **Type Safety Warning**: Operations are being conducted on generic structures without safe validation checks.\n\n" +
                    "### Security Issues\n" +
                    "- **Input Sanitization Missing**: External strings must be sanitized to prevent code injection attacks.\n" +
                    "- **Improper Error Handling**: Internal exception StackTraces are being output to standard channels, which reveals structural configurations.\n\n" +
                    "### Performance Suggestions\n" +
                    "- Cache recurring loops to prevent redundant memory allocations.\n" +
                    "- Use built-in libraries which compile into low-overhead assembly constructs.\n\n" +
                    "### Best Practices & Code Smells\n" +
                    "- Refactor long conditionals into separate boolean functions.\n" +
                    "- Rename single-character variables to descriptive identifiers.\n\n" +
                    "### Optimized Code\n" +
                    "```javascript\n" +
                    "// Optimized and Secure version\n" +
                    "function processInputData(inputString) {\n" +
                    "    if (!inputString || typeof inputString !== 'string') {\n" +
                    "        throw new Error('Invalid input exception');\n" +
                    "    }\n" +
                    "    const sanitized = inputString.replace(/[^a-zA-Z0-9 ]/g, '');\n" +
                    "    return sanitized.trim().toLowerCase();\n" +
                    "}\n" +
                    "```";
        } else if (promptText.contains("programming tutor")) {
            return "### Line-by-Line Explanation\n" +
                    "- **Line 1 (`function processInputData(...)`)**: Declares a new function that takes `inputString` as its argument.\n" +
                    "- **Line 2 (`if (!inputString...)`)**: Validates input boundaries to guard against `null`, `undefined`, or non-string parameters.\n" +
                    "- **Line 5 (`const sanitized = ...`)**: Applies a regular expression filter to strip out special character symbols.\n" +
                    "- **Line 6 (`return sanitized...`)**: Removes leading/trailing spaces and converts characters to lowercase before outputting.\n\n" +
                    "### Flow and Algorithms\n" +
                    "The function uses a guard clause to handle invalid cases immediately, then processes the data using string manipulation filters in a linear sequence.\n\n" +
                    "### Complexity Analysis\n" +
                    "- **Time Complexity**: \\(O(N)\\) where \\(N\\) is the length of `inputString` (due to Regex filtering scan).\n" +
                    "- **Space Complexity**: \\(O(N)\\) to store the sanitized intermediate results in memory.\n\n" +
                    "### Real-World Example\n" +
                    "Imagine a mailbox at a post office: first, it rejects anything that is not a standard envelope (Guard Clause). Next, it wipes off any mud or sticker tags from the envelope (Regex Sanitizer). Finally, it puts an official lowercase postmark on it and drops it in the bin (Return Output).\n\n" +
                    "### Summary\n" +
                    "A secure string sanitizer function utilizing standard regular expressions and defensive checking logic.";
        } else {
            return "```javascript\n" +
                    "// Generated code based on requirement\n" +
                    "function generateFibonacci(n) {\n" +
                    "    if (n <= 0) return [];\n" +
                    "    if (n === 1) return [0];\n" +
                    "    \n" +
                    "    const sequence = [0, 1];\n" +
                    "    while (sequence.length < n) {\n" +
                    "        const nextVal = sequence[sequence.length - 1] + sequence[sequence.length - 2];\n" +
                    "        sequence.push(nextVal);\n" +
                    "    }\n" +
                    "    return sequence;\n" +
                    "}\n" +
                    "```";
        }
    }
}
