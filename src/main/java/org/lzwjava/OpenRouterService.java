package org.lzwjava;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenRouterService {
    private static final Logger logger = LoggerFactory.getLogger(OpenRouterService.class);
    private final RestTemplate restTemplate;

    @Value("${openrouter.api.key:${OPENROUTER_API_KEY:}}")
    private String apiKey;

    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    @Value("${blog.default.model:mistralai/mistral-7b-instruct:free}")
    private String defaultModel;

    public OpenRouterService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public String callOpenRouterApi(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("OpenRouter API key is missing");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("HTTP-Referer", "https://github.com/lzwjava/blog-server");
        headers.set("X-Title", "Blog Server");

        Map<String, Object> requestBody =
                Map.of("model", defaultModel, "messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message =
                            (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            logger.error("Invalid response from OpenRouter: {}", response);
        } catch (Exception e) {
            logger.error("Error calling OpenRouter API", e);
        }
        return null;
    }
}
