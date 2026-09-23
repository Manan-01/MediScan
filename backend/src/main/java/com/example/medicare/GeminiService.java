package com.example.medicare;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MedicineParseResult parse(List<String> rawTexts) {
        MedicineParseResult result = new MedicineParseResult();
        try {
            String combinedText = String.join("\n---\n", rawTexts);

            String prompt = "You are extracting structured data from noisy OCR text scanned off a medicine package. "
                    + "The text may contain the medicine's brand name, dosage, a manufacture date (labeled MFG/MFD), "
                    + "and an expiry date (labeled EXP/EXPIRY/USE BY). Multiple OCR attempts of the same package are given below, "
                    + "separated by '---', since OCR is imperfect. "
                    + "Return the medicine's brand name (best guess, just the name without dosage) and the expiry date "
                    + "in MM/YYYY format — NOT the manufacture date. "
                    + "If you cannot confidently determine a field, return an empty string for it. "
                    + "OCR text:\n" + combinedText;

            Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "name", Map.of("type", "string"),
                            "expiry", Map.of("type", "string")
                    ),
                    "required", List.of("name", "expiry")
            );

            Map<String, Object> requestBody = Map.of(
                    "model", "gemini-3.8-flash",
                    "input", prompt,
                    "generation_config", Map.of("thinking_level", "low"),
                    "response_format", Map.of(
                            "type", "text",
                            "mime_type", "application/json",
                            "schema", schema
                    )
            );

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/interactions"))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                result.success = false;
                result.errorMessage = "Gemini API returned status " + response.statusCode() + ": " + response.body();
                return result;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode steps = root.path("steps");
            JsonNode lastStep = steps.get(steps.size() - 1);
            String modelText = lastStep.path("content").get(0).path("text").asText();

            JsonNode parsed = objectMapper.readTree(modelText);
            result.name = parsed.path("name").asText("");
            result.expiry = parsed.path("expiry").asText("");
            result.success = true;

        } catch (Exception e) {
            e.printStackTrace();
            result.success = false;
            result.errorMessage = e.toString();
        }
        return result;
    }
}
