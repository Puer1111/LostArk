package com.lostark.lostark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.dto.search.SearchCharacterDTO;
import com.lostark.lostark.dto.search.SearchExpeditionDTO;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${lostark.api.key}")
    private String apiKey;

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
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            return objectMapper.readValue(response.getBody(), SearchExpeditionDTO[].class);
        } catch (Exception e) {
            log.error("Error fetching or parsing expedition for character: {}", characterName, e);
            return new SearchExpeditionDTO[0];
        }
    }

    @Override
    public SearchCharacterDTO getCharacter(String characterName) {
        log.info("Service.getCharacter.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/armories/characters/")
                .path("{characterName}").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            SearchCharacterDTO dto = objectMapper.readValue(response.getBody(), SearchCharacterDTO.class);
            if (dto != null) {
                return dto;
            }
        } catch (Exception e) {
            log.error("Error fetching or parsing character data for: {}", characterName, e);
            return new SearchCharacterDTO(); // 실패 시 빈 객체 반환
        }
        return new SearchCharacterDTO();
    }

}


