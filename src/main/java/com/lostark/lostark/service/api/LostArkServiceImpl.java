package com.lostark.lostark.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.model.dto.character.CharacterEquipment;
import com.lostark.lostark.model.dto.character.search.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.search.SearchExpeditionDTO;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LostArkServiceImpl implements LostArkService {
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
            if (dto != null && dto.getCharacterEquipment() != null) {
                // 1. 필터링 로직: 보여주고 싶은 장비 타입 정의
                Set<String> desiredTypes = new HashSet<>(Arrays.asList(
                        "투구", "어깨", "상의", "하의", "장갑", "무기",
                        "목걸이", "귀걸이", "반지", "팔찌", "어빌리티 스톤"
                ));

                List<CharacterEquipment> filteredEquipment = dto.getCharacterEquipment().stream()
                        .filter(equip -> desiredTypes.contains(equip.getType()))
                        .collect(Collectors.toList());

                // 2. 정렬 로직: 필터링된 리스트를 정렬
                List<String> equipmentOrder = Arrays.asList(
                        "투구", "어깨", "상의", "하의", "장갑", "무기",
                        "목걸이", "귀걸이", "반지", "팔찌", "어빌리티 스톤"
                );
                Map<String, Integer> orderMap = new HashMap<>();
                for (int i = 0; i < equipmentOrder.size(); i++) {
                    orderMap.put(equipmentOrder.get(i), i);
                }

                filteredEquipment.sort(Comparator.comparingInt(equip ->
                        orderMap.getOrDefault(equip.getType(), Integer.MAX_VALUE)
                ));

                // 3. DTO에 최종 리스트 설정
                dto.setCharacterEquipment(filteredEquipment);

                return dto;
            }
        } catch (Exception e) {
            log.error("Error fetching or parsing character data for: {}", characterName, e);
            return new SearchCharacterDTO(); // 실패 시 빈 객체 반환
        }
        return new SearchCharacterDTO();
    }

}


