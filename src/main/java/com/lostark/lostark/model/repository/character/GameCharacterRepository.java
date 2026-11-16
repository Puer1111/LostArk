package com.lostark.lostark.model.repository.character;

import com.lostark.lostark.model.entity.character.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, String> {

    List<GameCharacter> findAllByCurrentNameIn(Collection<String> names);

    List<GameCharacter> findAllByExpedition_ExpeditionUid(String expeditionUid);
}
