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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
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
    private final org.springframework.beans.factory.ObjectProvider<LostArkService> lostArkServiceProvider;

    private LostArkService getSelf() {
        return lostArkServiceProvider.getIfAvailable();
    }

    /**
     * 개별 캐릭터 프로필 조회 (Redis 캐싱 적용 - profileCache)
     */
    @Override
    @Cacheable(value = "profileCache", key = "#characterName")
    public CharacterProfiles getCharacterProfile(String characterName) {
        log.info("Service.getCharacterProfile.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/armories/characters/")
                .path("{characterName}/profiles").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            return objectMapper.readValue(response.getBody(), CharacterProfiles.class);
        } catch (Exception e) {
            log.warn("Failed to fetch profile for character: {}", characterName);
            // 429 에러 발생 시 예외를 던져 캐싱되지 않도록 함 (다음에 다시 시도할 수 있게)
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("409"))) {
                throw new RuntimeException("API Limit or Conflict for character: " + characterName);
            }
            return null;
        }
    }

    @Override
    @LogExecutionTime
    @Cacheable(value = "expeditionCache", key = "#characterName")
    public SearchExpeditionDTO[] getExpedition(String characterName) {
        log.info("Service.getExpedition.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/characters/")
                .path("{characterName}/siblings").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            SearchExpeditionDTO[] siblings = objectMapper.readValue(response.getBody(), SearchExpeditionDTO[].class);

            if (siblings != null) {
                // 아이템 레벨 기준 정렬하여 상위 캐릭터 우선순위 부여
                List<SearchExpeditionDTO> siblingList = Arrays.asList(siblings);
                siblingList.sort((s1, s2) -> {
                    try {
                        double l1 = Double.parseDouble(s1.getItemAvgLevel().replace(",", ""));
                        double l2 = Double.parseDouble(s2.getItemAvgLevel().replace(",", ""));
                        return Double.compare(l2, l1);
                    } catch (Exception e) { return 0; }
                });

                // 전체 조회를 시도하되, 429 에러 발생 시 즉시 중단하여 부분 데이터만 반환
                for (SearchExpeditionDTO sibling : siblingList) {
                    try {
                        // getSelf()를 통해 프록시를 거쳐 호출함으로써 캐시가 작동하도록 함
                        CharacterProfiles profile = getSelf().getCharacterProfile(sibling.getCharacterName());
                        if (profile != null) {
                            sibling.setCharacterImage(profile.getCharacterImage());
                            sibling.setCombatPower(profile.getCombatPower());
                        }
                    } catch (Exception e) {
                        if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("409"))) {
                            log.error("Stop fetching expedition profiles due to API limit/conflict");
                            break; // 에러 발생 시 루프 탈출
                        }
                    }
                }
            }
            return siblings;
        } catch (Exception e) {
            log.error("Error fetching or parsing expedition for character: {}", characterName, e);
            throw new RuntimeException("원정대 데이터 조회 중 오류 발생", e);
        }
    }

    @Override
    @LogExecutionTime
    @Cacheable(value = "characterCache", key = "#characterName")
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
            
            // 원정대 정보는 이제 클라이언트에서 비동기로 별도 호출하므로 여기서 제거

            sortGems(dto);
            filterAndSortEquipment(dto);
            return dto;

        } catch (Exception e) {
            log.error("Error fetching or parsing character data for: {}", characterName, e);
            throw new RuntimeException("캐릭터 데이터 조회 중 오류 발생", e);
        }
    }

    private void sortGems(SearchCharacterDTO dto) {
        if (dto.getCharacterGems() == null || dto.getCharacterGems().getGems() == null) {
            return;
        }
        dto.getCharacterGems().getGems().sort(Comparator.comparing(gem -> {
            if (gem.getTooltip() == null) return 2;
            String effectType = gem.getTooltip().getPrimaryEffectType();
            switch (effectType) {
                case "INCREASE": return 0;
                case "DECREASE": return 1;
                default: return 2;
            }
        }));
    }

    private void filterAndSortEquipment(SearchCharacterDTO dto) {
        if (dto.getCharacterEquipment() == null) {
            return;
        }

        Set<String> desiredTypes = new HashSet<>(Arrays.asList(
                "투구", "어깨", "상의", "하의", "장갑", "무기",
                "목걸이", "귀걸이", "반지", "팔찌", "어빌리티 스톤"
        ));

        List<CharacterEquipment> filteredEquipment = dto.getCharacterEquipment().stream()
                .filter(equip -> desiredTypes.contains(equip.getType()))
                .collect(Collectors.toList());

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

        dto.setCharacterEquipment(filteredEquipment);
    }

    @Override
    @LogExecutionTime
    public SimplifiedCharacterDTO getSimplifiedCharacter(String characterName) {
        log.info("Service.getSimplifiedCharacter.characterName = {}", characterName);
        
        // Simplified 조회의 경우도 캐싱된 프로필 정보를 우선적으로 사용하도록 개선 가능
        CharacterProfiles profile = getCharacterProfile(characterName);

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
    }
}
