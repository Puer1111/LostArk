package com.lostark.lostark.controller.character;

import com.lostark.lostark.model.dto.character.RaidDto;
import com.lostark.lostark.service.character.RaidAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/character/api/raids")
@RequiredArgsConstructor
public class RaidController {

    private final RaidAdminService raidAdminService;

    @GetMapping
    public List<RaidDto> getRaids() {
        return raidAdminService.getAllRaids();
    }
}
