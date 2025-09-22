package com.lostark.lostark.service;

import com.lostark.lostark.dto.search.SearchExpeditionDTO;

public interface ApiService {
    /**
     * 캐릭터 검색
     * @param characterName
     * @return
     */
    SearchExpeditionDTO[] getExpedition(String characterName);
}
