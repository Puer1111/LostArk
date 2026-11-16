package com.lostark.lostark.controller.character;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.model.dto.character.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.SearchExpeditionDTO;
import com.lostark.lostark.model.dto.character.SimplifiedCharacterDTO;
import com.lostark.lostark.service.api.LostArkService;
import com.lostark.lostark.service.character.CharacterDataPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.lostark.lostark.model.dto.character.synergy.SynergyJobDto;
import com.lostark.lostark.service.character.SynergyAdminService;

import java.util.List;
import java.util.Arrays;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/character")
public class CharacterController {

    private final LostArkService apiService;
    private final SynergyAdminService synergyAdminService;
    private final CharacterDataPersistenceService characterDataPersistenceService;
    private final ObjectMapper objectMapper;

    /**
     * 캐릭터 상세 정보 조회
     */
    @GetMapping("/{characterName}")
    public String getCharacter(@PathVariable String characterName, Model model) {
        log.info("Controller.getCharacter.characterName {}", characterName);
        SearchCharacterDTO searchCharacterDTO = apiService.getCharacter(characterName);
        SearchExpeditionDTO[] siblings = new SearchExpeditionDTO[0];
        try {
            SearchExpeditionDTO[] response = apiService.getExpedition(characterName);
            if (response != null) {
                siblings = response;
            }
        } catch (Exception expeditionException) {
            log.warn("원정대 정보를 가져오지 못했습니다. 검색 캐릭터 정보만 저장합니다: {}",
                    characterName, expeditionException);
        }
        try {
            characterDataPersistenceService.saveSearchResult(
                    searchCharacterDTO,
                    objectMapper.valueToTree(searchCharacterDTO),
                    Arrays.asList(siblings));
        } catch (Exception persistenceException) {
            log.error("캐릭터 검색 결과를 DB에 저장하지 못했습니다: {}", characterName, persistenceException);
        }
        model.addAttribute("characterData", searchCharacterDTO);
        return "character/searchCharacter";
    }

    /**
     * 캐릭터 정보 강제 갱신 API (캐시 삭제)
     */
    @GetMapping("/api/refresh/{characterName}")
    @ResponseBody
    public ResponseEntity<Void> refreshCharacter(@PathVariable String characterName) {
        log.info("Controller.refreshCharacter.characterName: {}", characterName);
        apiService.refreshCharacter(characterName);
        return ResponseEntity.ok().build();
    }

    /**
     * 원정대 정보 비동기 조회를 위한 API (JSON 반환)
     */
    @GetMapping("/api/expedition/{characterName}")
    @ResponseBody
    public ResponseEntity<SearchExpeditionDTO[]> getExpedition(@PathVariable String characterName) {
        log.info("Controller.getExpedition.characterName: {}", characterName);
        SearchExpeditionDTO[] expeditions = apiService.getExpedition(characterName);
        return ResponseEntity.ok(expeditions);
    }

    /**
     * 파티 시뮬레이터용 간소화된 캐릭터 정보 API (JSON 반환)
     */
    @GetMapping("/api/simplified/{characterName}")
    @ResponseBody
    public ResponseEntity<SimplifiedCharacterDTO> getSimplifiedCharacter(@PathVariable String characterName) {
        log.info("Controller.getSimplifiedCharacter.characterName: {}", characterName);
        SimplifiedCharacterDTO dto = apiService.getSimplifiedCharacter(characterName);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * 원정대 캐릭터 일괄(Batch) 정보 조회를 위한 API (JSON 반환)
     */
    @PostMapping("/api/expedition/batch")
    @ResponseBody
    public ResponseEntity<java.util.List<SimplifiedCharacterDTO>> getSimplifiedCharactersBatch(@org.springframework.web.bind.annotation.RequestBody java.util.List<String> characterNames) {
        log.info("Controller.getSimplifiedCharactersBatch.count: {}", characterNames != null ? characterNames.size() : 0);
        java.util.List<SimplifiedCharacterDTO> dtos = apiService.getSimplifiedCharactersBatch(characterNames);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/class")
    public String getClassInfo() {
        return "character/synergyInformation";
    }

    @GetMapping("/party-simulator")
    public String getPartySimulator() {
        return "character/partySimulator";
    }

    /**
     * 파티 시뮬레이터용 전체 직업 시너지 조회 공용 API
     */
    @GetMapping("/api/synergies")
    @ResponseBody
    public ResponseEntity<List<SynergyJobDto>> getPublicSynergies() {
        return ResponseEntity.ok(synergyAdminService.getAllSynergies());
    }

}
