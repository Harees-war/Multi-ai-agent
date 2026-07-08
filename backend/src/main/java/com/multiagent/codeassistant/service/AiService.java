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
                "Do not include any code comments, inline comments, or documentation comments within the generated code. The code must be completely free of comments. " +
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

        String[] models = {
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-flash-latest",
            "gemini-pro-latest"
        };

        Exception lastException = null;

        for (int attempt = 0; attempt < models.length; attempt++) {
            String currentModel = models[attempt];
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
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent?key=" + apiKey;

                logger.info("Calling Gemini API with model: " + currentModel + " (Attempt " + (attempt + 1) + ")");
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
                lastException = e;
                logger.warn("Attempt " + (attempt + 1) + " failed for model " + currentModel + ": " + e.getMessage());
                if (attempt < models.length - 1) {
                    try {
                        Thread.sleep(800); // Delay before trying next fallback model
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        logger.error("All Gemini API fallback models failed. Last exception: " + lastException.getMessage(), lastException);
        return "### Error calling Gemini API\n" +
                "An error occurred: " + lastException.getMessage() + "\n\n" +
                "#### Here is a simulated response:\n" +
                getMockResponse(promptText);
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
                "1. **Syntax Warnings**: Found minor stylistic issues corresponding to " + lang.toUpperCase() + " standard setups.\n" +
                "2. **Type Coercion Risk**: Ensure dynamic comparisons are properly validated.\n\n" +
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
