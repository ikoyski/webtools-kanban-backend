package com.ikoyki.webtools.kanban.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurnstileService {

    private final RestTemplate restTemplate;

    @Value("${cloudflare.turnstile.secret}")
    private String SECRET_KEY;

    @Value("${cloudflare.turnstile.siteverify-url}")
    private String VERIFY_URL;

    public boolean verify(String token, String remoteIp) {
        if (token == null || token.isBlank()) {
            log.warn("Turnstile token is missing or empty");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("secret", SECRET_KEY);
            map.add("response", token);
            map.add("remoteip", remoteIp);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(VERIFY_URL, request, Map.class);

            if (response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                return success != null && success;
            }
        } catch (Exception e) {
            log.error("Error verifying Turnstile token: {}", e.getMessage());
        }

        return false;
    }
}
