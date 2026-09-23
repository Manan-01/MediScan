package com.example.medicare;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/api/parse-medicine")
    public MedicineParseResult parseMedicine(@RequestBody MedicineParseRequest request) {
        return geminiService.parse(request.rawTexts);
    }
}
