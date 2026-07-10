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

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.api.model}")
    private String apiModel;

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
                "Do not include any code comments, inline comments, or documentation comments within the generated code. The code must be completely free of comments. " +
                "Language: " + language + ". Requirement: " + requirement + ". Return only the code inside Markdown code blocks. Do not add explanations or surrounding discussion.";
        
        String response = callOpenRouter(systemInstruction);
        saveHistory(email, "Generator", language, requirement, response);
        return response;
    }

    public String reviewCode(String email, String language, String code) {
        String systemInstruction = "You are a senior code reviewer. Review this " + language + " code:\n\n" + code + "\n\n" +
                "Return a detailed report in Markdown format with the following headings:\n" +
                "### Bug Detection\n" +
                "For every identified bug or compile error, you MUST explicitly list the Line Number, the exact code line content containing the error, a description of the issue, and provide a corrected version code block directly inside this section for that bug.\n" +
                "### Security Issues\n" +
                "### Performance Suggestions\n" +
                "### Best Practices & Code Smells\n" +
                "### Optimized Code\n" +
                "Include the fully corrected and optimized version of the program inside Markdown code blocks.";

        String response = callOpenRouter(systemInstruction);
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

        String response = callOpenRouter(systemInstruction);
        saveHistory(email, "Explainer", language, code.length() > 100 ? code.substring(0, 100) + "..." : code, response);
        return response;
    }

    private void saveHistory(String email, String agent, String language, String prompt, String response) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        History history = new History(user, agent, language, prompt, response);
        historyRepository.save(history);
    }

    private String callOpenRouter(String promptText) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("${")) {
            logger.warn("OPENROUTER_API_KEY is not configured. Returning fallback mock response.");
            return getMockResponse(promptText);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", "http://localhost:8080");
            headers.set("X-Title", "AI Multi-Agent Code Assistant");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", apiModel);
            requestBody.put("max_tokens", 2000);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", promptText);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("Calling OpenRouter API with model: " + apiModel);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(apiUrl, request, Map.class);
            
            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                Map body = responseEntity.getBody();
                List choices = (List) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map message = (Map) firstChoice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            throw new RuntimeException("Unexpected response format from OpenRouter API");
        } catch (Exception e) {
            logger.error("OpenRouter API call failed: " + e.getMessage(), e);
            
            String errorMessage = e.getMessage() != null ? e.getMessage() : "";
            String userFriendlyErrorHeader = "### ⚠️ API Connection Issue\n" +
                    "Could not complete request to OpenRouter API. Reverting to local fallback mode.\n\n";

            if (errorMessage.contains("429") || errorMessage.contains("RESOURCE_EXHAUSTED")) {
                userFriendlyErrorHeader = "### ⚠️ API Quota Exceeded (Rate Limit)\n" +
                        "You have exceeded your OpenRouter API request quota. Please wait a few seconds before retrying.\n\n";
            } else if (errorMessage.contains("503") || errorMessage.contains("UNAVAILABLE")) {
                userFriendlyErrorHeader = "### ⚠️ OpenRouter Service Overloaded\n" +
                        "OpenRouter servers are currently experiencing high traffic. Reverting to local fallback mode.\n\n";
            }

            return userFriendlyErrorHeader +
                    "#### Here is a simulated response:\n" +
                    getMockResponse(promptText);
        }
    }

    private String getMockResponse(String promptText) {
        String lowerPrompt = promptText.toLowerCase();
        
        // Extract language from system prompt instruction
        String lang = "javascript"; // fallback
        if (lowerPrompt.contains("language: java") || lowerPrompt.contains("in java")) lang = "java";
        else if (lowerPrompt.contains("language: python") || lowerPrompt.contains("in python")) lang = "python";
        else if (lowerPrompt.contains("language: c++") || lowerPrompt.contains("in c++")) lang = "cpp";
        else if (lowerPrompt.contains("language: html") || lowerPrompt.contains("in html")) lang = "html";
        else if (lowerPrompt.contains("language: css") || lowerPrompt.contains("in css")) lang = "css";
        else if (lowerPrompt.contains("language: sql") || lowerPrompt.contains("in sql")) lang = "sql";

        if (lowerPrompt.contains("senior code reviewer")) {
            return getMockReviewReport(lang);
        } else if (lowerPrompt.contains("programming tutor")) {
            return getMockExplanationReport(lang);
        } else {
            return getMockGeneratedCode(lang, lowerPrompt);
        }
    }

    private String getMockReviewReport(String lang) {
        return "### Bug Detection\n" +
                "1. **Potential Null Reference Exception (Line 2)**\n" +
                "   - **Line Content**: `if (input.equals(\"\"))`\n" +
                "   - **Issue**: Calling `.equals()` on `input` without checking if `input` is `null` first can cause a NullPointerException.\n" +
                "   - **Corrected Code**:\n" +
                "     ```" + lang + "\n" +
                "     if (input == null || input.equals(\"\"))\n" +
                "     ```\n" +
                "2. **Type Coercion Warning (Line 5)**\n" +
                "   - **Line Content**: `if (count == \"0\")`\n" +
                "   - **Issue**: Comparing numeric variable `count` to string value `\"0\"` may cause coercion errors.\n" +
                "   - **Corrected Code**:\n" +
                "     ```" + lang + "\n" +
                "     if (count == 0)\n" +
                "     ```\n\n" +
                "### Security Issues\n" +
                "- **Unsanitized Variable Scans**: Ensure user parameters are checked against boundary constraints.\n\n" +
                "### Performance Suggestions\n" +
                "- Avoid unnecessary object allocation loops inside iterative conditions.\n\n" +
                "### Best Practices & Code Smells\n" +
                "- Refactor code structures into standalone, single-purpose functions.\n\n" +
                "### Optimized Code\n" +
                "```" + lang + "\n" +
                "// Optimized " + lang.toUpperCase() + " version\n" +
                getGenericCodeSnippet(lang) + "\n" +
                "```";
    }

    private String getMockExplanationReport(String lang) {
        return "### Line-by-Line Explanation\n" +
                "- **Line 1**: Declares the execution block in " + lang.toUpperCase() + ".\n" +
                "- **Line 2**: Initializes variables and baseline counters.\n" +
                "- **Line 3**: Implements iterative loops to process input fields.\n\n" +
                "### Flow and Algorithms\n" +
                "The algorithm processes the input sequence line-by-line and applies data filters before returning the calculated value.\n\n" +
                "### Complexity Analysis\n" +
                "- **Time Complexity**: \\(O(N)\\) linear runtime scanning inputs.\n" +
                "- **Space Complexity**: \\(O(1)\\) constant storage allocations.\n\n" +
                "### Real-World Example\n" +
                "Like a ticket counter checker scanning one passenger ticket at a time in sequence.\n\n" +
                "### Summary\n" +
                "A clean utility code block written in " + lang.toUpperCase() + ".";
    }

    private String getMockGeneratedCode(String lang, String prompt) {
        if (prompt.contains("factorial")) {
            if (lang.equals("java")) {
                return "```java\n" +
                        "public class FactorialUtility {\n" +
                        "    public static long getFactorial(int n) {\n" +
                        "        if (n <= 1) return 1;\n" +
                        "        return n * getFactorial(n - 1);\n" +
                        "    }\n" +
                        "}\n" +
                        "```";
            } else if (lang.equals("python")) {
                return "```python\n" +
                        "def get_factorial(n):\n" +
                        "    if n <= 1:\n" +
                        "        return 1\n" +
                        "    return n * get_factorial(n - 1)\n" +
                        "```";
            } else if (lang.equals("sql")) {
                return "```sql\n" +
                        "WITH RECURSIVE FactorialCTE AS (\n" +
                        "    SELECT 1 AS n, 1 AS fact\n" +
                        "    UNION ALL\n" +
                        "    SELECT n + 1, (n + 1) * fact FROM FactorialCTE WHERE n < 10\n" +
                        ")\n" +
                        "SELECT * FROM FactorialCTE;\n" +
                        "```";
            } else {
                return "```javascript\n" +
                        "function getFactorial(n) {\n" +
                        "    if (n <= 1) return 1;\n" +
                        "    return n * getFactorial(n - 1);\n" +
                        "}\n" +
                        "```";
            }
        } else if (prompt.contains("prime")) {
            if (lang.equals("java")) {
                return "```java\n" +
                        "public class PrimeCheck {\n" +
                        "    public static boolean isPrime(int n) {\n" +
                        "        if (n <= 1) return false;\n" +
                        "        for (int i = 2; i <= Math.sqrt(n); i++) {\n" +
                        "            if (n % i == 0) return false;\n" +
                        "        }\n" +
                        "        return true;\n" +
                        "    }\n" +
                        "}\n" +
                        "```";
            } else if (lang.equals("python")) {
                return "```python\n" +
                        "import math\n" +
                        "def is_prime(n):\n" +
                        "    if n <= 1:\n" +
                        "        return False\n" +
                        "    for i in range(2, int(math.sqrt(n)) + 1):\n" +
                        "        if n % i == 0:\n" +
                        "            return False\n" +
                        "    return True\n" +
                        "```";
            } else {
                return "```javascript\n" +
                        "function isPrime(n) {\n" +
                        "    if (n <= 1) return false;\n" +
                        "    for (let i = 2; i <= Math.sqrt(n); i++) {\n" +
                        "        if (n % i === 0) return false;\n" +
                        "    }\n" +
                        "    return true;\n" +
                        "}\n" +
                        "```";
            }
        } else if (prompt.contains("fibonacci")) {
            if (lang.equals("java")) {
                return "```java\n" +
                        "import java.util.ArrayList;\n" +
                        "public class Fibonacci {\n" +
                        "    public static ArrayList<Integer> generate(int count) {\n" +
                        "        ArrayList<Integer> seq = new ArrayList<>();\n" +
                        "        if (count <= 0) return seq;\n" +
                        "        seq.add(0);\n" +
                        "        if (count == 1) return seq;\n" +
                        "        seq.add(1);\n" +
                        "        while (seq.size() < count) {\n" +
                        "            seq.add(seq.get(seq.size() - 1) + seq.get(seq.size() - 2));\n" +
                        "        }\n" +
                        "        return seq;\n" +
                        "    }\n" +
                        "}\n" +
                        "```";
            } else if (lang.equals("python")) {
                return "```python\n" +
                        "def generate_fibonacci(count):\n" +
                        "    if count <= 0: return []\n" +
                        "    if count == 1: return [0]\n" +
                        "    seq = [0, 1]\n" +
                        "    while len(seq) < count:\n" +
                        "        seq.append(seq[-1] + seq[-2])\n" +
                        "    return seq\n" +
                        "```";
            } else {
                return "```javascript\n" +
                        "function generateFibonacci(count) {\n" +
                        "    if (count <= 0) return [];\n" +
                        "    if (count === 1) return [0];\n" +
                        "    const seq = [0, 1];\n" +
                        "    while (seq.length < count) {\n" +
                        "        seq.push(seq[seq.length - 1] + seq[seq.length - 2]);\n" +
                        "    }\n" +
                        "    return seq;\n" +
                        "}\n" +
                        "```";
            }
        }

        return getGenericCodeSnippet(lang);
    }

    private String getGenericCodeSnippet(String lang) {
        if (lang.equals("java")) {
            return "```java\n" +
                    "public class HelperUtility {\n" +
                    "    public static void execute() {\n" +
                    "        System.out.println(\"Completed action inside utility\");\n" +
                    "    }\n" +
                    "}\n" +
                    "```";
        } else if (lang.equals("python")) {
            return "```python\n" +
                    "def execute():\n" +
                    "    print(\"Completed action inside utility\")\n" +
                    "\n" +
                    "execute()\n" +
                    "```";
        } else if (lang.equals("sql")) {
            return "```sql\n" +
                    "SELECT id, username, email FROM users WHERE role = 'USER';\n" +
                    "```";
        } else if (lang.equals("html")) {
            return "```html\n" +
                    "<div class=\"container\">\n" +
                    "    <h1>Project Multi-Agent Helper</h1>\n" +
                    "    <p>Rendering mock content panel</p>\n" +
                    "</div>\n" +
                    "```";
        } else if (lang.equals("css")) {
            return "```css\n" +
                    ".container {\n" +
                    "    display: flex;\n" +
                    "    justify-content: center;\n" +
                    "    background: rgba(255,255,255,0.05);\n" +
                    "}\n" +
                    "```";
        } else {
            return "```javascript\n" +
                    "function execute() {\n" +
                    "    console.log(\"Completed action inside utility\");\n" +
                    "}\n" +
                    "```";
        }
    }
}
