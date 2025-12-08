package com.lostark.lostark.controller.character;


import com.lostark.lostark.model.dto.character.search.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.search.SearchExpeditionDTO;
import com.lostark.lostark.service.api.LostArkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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
        SearchCharacterDTO searchCharacterDTO = apiService.getCharacter(characterName);
        model.addAttribute("characterData", searchCharacterDTO);
        return "character/searchCharacter";
    }

    // 캐릭터 검색 - 원정대
    @GetMapping("/allExpedition/{characterName}")
    public String getExpedition(@PathVariable String characterName, Model model) {
        log.info("Controller.getExpedition.characterName = {}", characterName);
        SearchExpeditionDTO[] characterProfiles = apiService.getExpedition(characterName);
        model.addAttribute("Expeditions", characterProfiles);
        return "character/allExpedition";
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
