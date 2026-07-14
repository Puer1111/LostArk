package com.lostark.lostark.service.api;

import com.lostark.lostark.model.dto.character.CharacterProfiles;
import com.lostark.lostark.model.dto.character.search.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.search.SearchExpeditionDTO;
import com.lostark.lostark.model.dto.character.search.SimplifiedCharacterDTO;
import com.lostark.lostark.model.dto.LostArkCalendar;

import com.lostark.lostark.model.dto.LostArkNewsDto;
import com.lostark.lostark.model.dto.LostArkEventDto;

import java.util.List;

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
     * 캐릭터 상세 프로필 조회 (캐시 적용 대상)
     * @param characterName
     * @return
     */
    CharacterProfiles getCharacterProfile(String characterName);

    /**
     * 파티 시뮬레이터를 위한 간소화된 캐릭터 정보 조회 ( 캐릭터 명 )
     * @param characterName
     * @return SimplifiedCharacterDTO
     */
    SimplifiedCharacterDTO getSimplifiedCharacter(String characterName);

    /**
     * 캐릭터 정보 강제 갱신 (캐시 삭제)
     * @param characterName
     */
    void refreshCharacter(String characterName);

    /**
     * 게임 캘린더 정보 조회
     * @return List<LostArkCalendar>
     */
    List<LostArkCalendar> getCalendar();

    /**
     * 오늘 나타나는 주요 일정(모험 섬, 카오스게이트, 필드보스) 정보 조회
     * @return List<LostArkCalendar>
     */
    List<LostArkCalendar> getTodayEvents();

    /**
     * 최근 검색한 캐릭터 정보 조회
     * @return List<SimplifiedCharacterDTO>
     */
    List<SimplifiedCharacterDTO> getRecentCharacters();

    /**
     * 로스트아크 공지사항 조회
     * @return List<LostArkNewsDto>
     */
    List<LostArkNewsDto> getNewsNotices();

    /**
     * 로스트아크 이벤트 조회
     * @return List<LostArkEventDto>
     */
    List<LostArkEventDto> getNewsEvents();
}
