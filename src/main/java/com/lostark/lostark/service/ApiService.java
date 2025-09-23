package com.lostark.lostark.service;

import com.lostark.lostark.dto.search.SearchCharacterDTO;
import com.lostark.lostark.dto.search.SearchExpeditionDTO;

public interface ApiService {
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
}
