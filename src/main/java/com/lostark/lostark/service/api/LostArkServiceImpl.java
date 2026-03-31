package com.lostark.lostark.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.config.aspect.LogExecutionTime;
import com.lostark.lostark.config.headers.HeaderUtils;
import com.lostark.lostark.model.dto.character.CharacterEquipment;
import com.lostark.lostark.model.dto.character.CharacterProfiles;
import com.lostark.lostark.model.dto.character.search.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.search.SearchExpeditionDTO;
import com.lostark.lostark.model.dto.character.search.SimplifiedCharacterDTO;
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
    private final HeaderUtils headerUtils;

    @Override
    @LogExecutionTime
    public SearchExpeditionDTO[] getExpedition(String characterName) {
        log.info("Service.getExpedition.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/characters/")
                .path("{characterName}/siblings").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            SearchExpeditionDTO[] siblings = objectMapper.readValue(response.getBody(), SearchExpeditionDTO[].class);

            if (siblings != null) {
                // 각 원정대원별로 프로필 정보를 추가로 조회하여 이미지와 전투력을 보완
                // API 호출 제한이 있으므로 원정대원이 많을 경우를 대비해 순차적으로 처리하거나, 
                // 필요에 따라 병렬 스트림(parallelStream)을 고려할 수 있습니다.
                for (SearchExpeditionDTO sibling : siblings) {
                    try {
                        URI profileUri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/armories/characters/")
                                .path("{characterName}/profiles").encode().buildAndExpand(sibling.getCharacterName()).toUri();
                        ResponseEntity<String> profileRes = restTemplate.exchange(profileUri, HttpMethod.GET, entity, String.class);
                        CharacterProfiles profile = objectMapper.readValue(profileRes.getBody(), CharacterProfiles.class);
                        if (profile != null) {
                            sibling.setCharacterImage(profile.getCharacterImage());
                            sibling.setCombatPower(profile.getCombatPower());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to fetch profile for sibling: {}", sibling.getCharacterName());
                    }
                }
            }
            return siblings;
        } catch (Exception e) {
            log.error("Error fetching or parsing expedition for character: {}", characterName, e);
            throw new RuntimeException("로스트아크 API 호출 또는 원정대 데이터 파싱 중 오류 발생", e);
        }
    }

    @Override
    @LogExecutionTime
    public SearchCharacterDTO getCharacter(String characterName) {
        log.info("Service.getCharacter.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/armories/characters/")
                .path("{characterName}").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            SearchCharacterDTO dto = objectMapper.readValue(response.getBody(), SearchCharacterDTO.class);
            if (dto == null) {
                return new SearchCharacterDTO();
            }
            
            // 원정대 정보 조회 및 추가
            SearchExpeditionDTO[] expeditionArray = getExpedition(characterName);
            if (expeditionArray != null) {
                List<SearchExpeditionDTO> expeditionList = new ArrayList<>(Arrays.asList(expeditionArray));
                
                // 아이템 레벨 내림차순 정렬 (높은 레벨이 먼저 오도록)
                expeditionList.sort((s1, s2) -> {
                    double level1 = Double.parseDouble(s1.getItemAvgLevel().replace(",", ""));
                    double level2 = Double.parseDouble(s2.getItemAvgLevel().replace(",", ""));
                    return Double.compare(level2, level1); // 내림차순
                });
                
                dto.setExpeditions(expeditionList);
            }

            sortGems(dto);
            filterAndSortEquipment(dto);
            return dto;

        } catch (Exception e) {
            log.error("Error fetching or parsing character data for: {}", characterName, e);
            throw new RuntimeException("로스트아크 API 호출 또는 캐릭터 데이터 파싱 중 오류 발생", e);
        }
    }

    private void sortGems(SearchCharacterDTO dto) {
        if (dto.getCharacterGems() == null || dto.getCharacterGems().getGems() == null) {
            return;
        }
        dto.getCharacterGems().getGems().sort(Comparator.comparing(gem -> {
            if (gem.getTooltip() == null) return 2; // Gems without tooltips go last
            String effectType = gem.getTooltip().getPrimaryEffectType();
            switch (effectType) {
                case "INCREASE": return 0; // '증가' (Increase) effects first
                case "DECREASE": return 1; // '감소' (Decrease) effects second
                default: return 2;         // Others/None last
            }
        }));
    }

    private void filterAndSortEquipment(SearchCharacterDTO dto) {
        if (dto.getCharacterEquipment() == null) {
            return;
        }

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
    }

    @Override
    @LogExecutionTime
    public SimplifiedCharacterDTO getSimplifiedCharacter(String characterName) {
        log.info("Service.getSimplifiedCharacter.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/armories/characters/")
                .path("{characterName}/profiles").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            CharacterProfiles profile = objectMapper.readValue(response.getBody(), CharacterProfiles.class);

            if (profile == null) {
                return null;
            }

            return SimplifiedCharacterDTO.builder()
                    .characterImage(profile.getCharacterImage())
                    .characterName(profile.getCharacterName())
                    .characterClassName(profile.getCharacterClassName())
                    .combatPower(profile.getCombatPower())
                    .itemLevel(profile.getItemAvgLevel())
                    .build();

        } catch (Exception e) {
            log.error("Error fetching simplified character data for: {}", characterName, e);
            throw new RuntimeException("로스트아크 API 호출 중 오류 발생", e);
        }
    }
}


