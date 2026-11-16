package com.lostark.lostark.model.repository.character;

import com.lostark.lostark.model.entity.character.CharacterNameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterNameHistoryRepository extends JpaRepository<CharacterNameHistory, Long> {

    List<CharacterNameHistory> findAllByServerNameAndCharacterNameAndValidToIsNull(
            String serverName,
            String characterName);

    List<CharacterNameHistory> findAllByCharacter_CharacterUidAndValidToIsNull(String characterUid);
}
