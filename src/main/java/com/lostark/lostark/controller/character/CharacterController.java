package com.lostark.lostark.controller.character;


import com.lostark.lostark.model.dto.character.search.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.search.SearchExpeditionDTO;
import com.lostark.lostark.model.dto.character.search.SimplifiedCharacterDTO;
import com.lostark.lostark.service.api.LostArkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/character")
public class CharacterController {

    private final LostArkService apiService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // 캐릭터 검색
    @GetMapping("/{characterName}")
    public String getCharacter(@PathVariable String characterName, Model model) {
        log.info("Controller.getCharacter.characterName {}", characterName);
        log.debug("Received characterName for getCharacter: {}", characterName); // 디버그 로그 추가
        SearchCharacterDTO searchCharacterDTO = apiService.getCharacter(characterName);
        model.addAttribute("characterData", searchCharacterDTO);
        return "character/searchCharacter";
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

    @GetMapping("/class")
    public String getClassInfo() {
        return "character/synergyInformation";
    }

    @GetMapping("/party-simulator")
    public String getPartySimulator() {
        return "character/partySimulator";
    }

}
