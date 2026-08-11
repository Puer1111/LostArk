package com.lostark.lostark.model.dto.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimplifiedCharacterDTO {
    private String characterImage;
    private String characterName;
    private String characterClassName;
    private String combatPower;
    private String itemLevel;
}
