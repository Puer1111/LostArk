package com.lostark.lostark.service.api;

import com.lostark.lostark.model.dto.character.search.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.search.SearchExpeditionDTO;

public interface LostArkService {
    /**
     * 원정대 검색 ( 캐릭터 명 )
     * @param characterName
     * @return
     */
    SearchExpeditionDTO[] getExpedition(String characterName);

    /**
     * 캐릭터 검색 프로필 ( 캐릭터 명 )
     * @param characterName
     * @return
     */
    SearchCharacterDTO getCharacter(String characterName);

    /**
     * 파티 시뮬레이터를 위한 간소화된 캐릭터 정보 조회 ( 캐릭터 명 )
     * @param characterName
     * @return SimplifiedCharacterDTO
     */
    com.lostark.lostark.model.dto.character.search.SimplifiedCharacterDTO getSimplifiedCharacter(String characterName);
}
