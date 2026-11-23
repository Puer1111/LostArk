package com.lostark.lostark.model.dto.character;

import java.util.List;
import com.lostark.lostark.model.entity.character.RaidCategory;
import com.lostark.lostark.model.entity.character.RaidDifficulty;

public record RaidDto(Long id, String raidName, RaidCategory raidCategory, Integer memberCount, Integer partyCount, List<GateDto> gates) {

    public record GateDto(Long id, Integer gateNumber, String bossName, List<DifficultyDto> difficulties) {}

    public record DifficultyDto(Long id, RaidDifficulty difficulty, String raidHp) {}
}
