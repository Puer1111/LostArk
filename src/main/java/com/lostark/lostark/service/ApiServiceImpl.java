package com.lostark.lostark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.dto.search.SearchCharacterDTO;
import com.lostark.lostark.dto.search.SearchExpeditionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@Slf4j
public class ApiServiceImpl implements ApiService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${lostark.api.key}")
    private String apiKey;

    public ApiServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // New private method to create HttpHeaders
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "bearer " + apiKey);
        return headers;
    }

    @Override
    public SearchExpeditionDTO[] getExpedition(String characterName) {
        log.info("Service.getExpedition.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/characters/")
                .path("{characterName}/siblings").encode().buildAndExpand(characterName).toUri();
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        String jsonResponse = response.getBody();
        System.out.println("Raw JSON Response: " + jsonResponse);

        try {
            SearchExpeditionDTO[] profiles = objectMapper.readValue(jsonResponse, SearchExpeditionDTO[].class);
            if (profiles != null) {
                return profiles;
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
        }

        return new SearchExpeditionDTO[0]; // 실패 시 빈 배열 반환
    }

    @Override
    public SearchCharacterDTO getCharacter(String characterName) {
        log.info("Service.getCharacter.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/armories/characters/")
                .path("{characterName}").encode().buildAndExpand(characterName).toUri();
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        String jsonResponse = response.getBody();
        System.out.println("Raw JSON Response: " + jsonResponse);

        try {
            SearchCharacterDTO dto = objectMapper.readValue(jsonResponse, SearchCharacterDTO.class);
            if (dto != null) {
                return dto;
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
        }
        return new SearchCharacterDTO();
    }
}
