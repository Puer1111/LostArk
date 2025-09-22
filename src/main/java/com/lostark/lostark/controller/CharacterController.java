package com.lostark.lostark.controller;


import com.lostark.lostark.dto.search.SearchExpeditionDTO;
import com.lostark.lostark.service.ApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CharacterController {

    private final ApiService apiService;

    @GetMapping("/character")
    public String test() {
        return "character";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/character/{characterName}")
    public String getCharacter(@PathVariable String characterName, Model model) {
        log.info("Controller.getCharacter.characterName {}", characterName);

        return "character";
    }
    @GetMapping("/character/allExpedition/{characterName}")
    public String getExpedition(@PathVariable String characterName, Model model) {
        log.info("Controller.getExpedition.characterName = {}", characterName);
        SearchExpeditionDTO[] characterProfiles = apiService.getExpedition(characterName);
        model.addAttribute("characters", characterProfiles);
        return "character";
    }
}
