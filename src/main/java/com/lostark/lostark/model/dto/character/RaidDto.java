package com.lostark.lostark.model.dto.character;

public record RaidDto(
        Long id,
        String raidName,
        String raidCategory,
        Integer memberCount,
        Integer partyCount
) {
}
