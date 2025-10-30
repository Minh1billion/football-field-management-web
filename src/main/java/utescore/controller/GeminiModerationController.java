package utescore.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/user/api")
public class GeminiModerationController {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/list-gemini-models")
    public ResponseEntity<?> listGeminiModels() {
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + geminiApiKey;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", e.getMessage(),
                            "hint", "Kiểm tra API key tại https://makersuite.google.com/app/apikey"
                    ));
        }
    }


    @PostMapping("/check-comment-gemini")
    public ResponseEntity<Map<String, Object>> checkCommentGemini(@RequestBody Map<String, String> body) {
        String content = body.get("content");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content không được để trống"));
        }

        // 🔹 Dùng model mới nhất: Gemini 2.5 Flash
        String modelName = "gemini-2.0-flash";
        String url = String.format(
        	    "https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s",
        	    modelName, geminiApiKey
        	);

        // 🔹 Prompt AI để kiểm duyệt
        String prompt = String.format(
                """
                Phân tích bình luận sau và trả về CHÍNH XÁC JSON:
                {"bad": true/false, "reason": "giải thích ngắn gọn"}

                Quy tắc:
                - bad=true nếu có: ngôn từ xúc phạm, bạo lực, phân biệt, tục tĩu, tình dục, spam
                - bad=false nếu bình thường

                Bình luận: "%s"
                """,
                content.replace("\"", "\\\"")
        );

        // 🔹 Request body chuẩn cho Gemini v1beta
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("role", "user",
                                "parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 200
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Gemini API không trả về dữ liệu"));
            }

            String aiText = extractTextFromGeminiResponse(responseBody);
            Map<String, Object> moderationResult = parseAIResponse(aiText);

            return ResponseEntity.ok(moderationResult);

        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Lỗi kết nối Gemini API: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi xử lý: " + e.getMessage()));
        }
    }

    private String extractTextFromGeminiResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty())
                throw new RuntimeException("Không có candidates trong response");

            Object contentObj = candidates.get(0).get("content");

            // ✅ Trường hợp 1: content là Map
            if (contentObj instanceof Map<?, ?> contentMap) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                if (parts != null) {
                    for (Map<String, Object> p : parts) {
                        Object t = p.get("text");
                        if (t != null && !t.toString().trim().isEmpty()) {
                            return t.toString().trim();
                        }
                    }
                }
            }

            // ✅ Trường hợp 2: content là List (một số model Gemini mới)
            if (contentObj instanceof List<?> contentList) {
                for (Object obj : contentList) {
                    if (obj instanceof Map<?, ?> item) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) item.get("parts");
                        if (parts != null) {
                            for (Map<String, Object> p : parts) {
                                Object t = p.get("text");
                                if (t != null && !t.toString().trim().isEmpty()) {
                                    return t.toString().trim();
                                }
                            }
                        }
                    }
                }
            }

            // ✅ Thử đọc trực tiếp "output_text" (một số bản trả về field khác)
            Object alt = candidates.get(0).get("output_text");
            if (alt != null) return alt.toString().trim();

            throw new RuntimeException("Không tìm thấy text trong parts hoặc output_text");
        } catch (Exception e) {
            throw new RuntimeException("Cấu trúc response không đúng: " + e.getMessage());
        }
    }

    private Map<String, Object> parseAIResponse(String aiText) {
        try {
            String cleanedText = aiText
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode jsonNode = objectMapper.readTree(cleanedText);
            boolean bad = jsonNode.path("bad").asBoolean(false);
            String reason = jsonNode.path("reason").asText("Không có lý do");

            return Map.of(
                    "flagged", bad,
                    "reason", reason,
                    "raw", aiText
            );

        } catch (Exception e) {
            boolean bad = aiText.toLowerCase().contains("\"bad\": true")
                    || aiText.toLowerCase().contains("bad: true");

            return Map.of(
                    "flagged", bad,
                    "reason", "Không parse được JSON, fallback sang text",
                    "raw", aiText,
                    "warning", "JSON format không hợp lệ"
            );
        }
    }
}
