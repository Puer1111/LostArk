package com.lostark.lostark.model.repository.character;

import com.lostark.lostark.model.entity.character.CharacterBestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CharacterBestSnapshotRepository extends JpaRepository<CharacterBestSnapshot, String> {

    @Modifying
    @Query(value = "INSERT INTO character_best_snapshot "
            + "(character_uid, combat_power, engravings_json, gems_json, ark_passive_json, ark_grid_json, skills_json, avatars_json, "
            + "captured_name, captured_server, captured_class, captured_character_level, captured_item_level, captured_at) "
            + "VALUES (:characterUid, :combatPower, :engravingsJson, :gemsJson, :arkPassiveJson, :arkGridJson, :skillsJson, :avatarsJson, "
            + ":capturedName, :capturedServer, :capturedClass, :capturedCharacterLevel, :capturedItemLevel, :capturedAt) "
            + "ON DUPLICATE KEY UPDATE "
            + "engravings_json = IF(VALUES(combat_power) >= combat_power, VALUES(engravings_json), engravings_json), "
            + "gems_json = IF(VALUES(combat_power) >= combat_power, VALUES(gems_json), gems_json), "
            + "ark_passive_json = IF(VALUES(combat_power) >= combat_power, VALUES(ark_passive_json), ark_passive_json), "
            + "ark_grid_json = IF(VALUES(combat_power) >= combat_power, VALUES(ark_grid_json), ark_grid_json), "
            + "skills_json = IF(VALUES(combat_power) >= combat_power, VALUES(skills_json), skills_json), "
            + "avatars_json = IF(VALUES(combat_power) >= combat_power, VALUES(avatars_json), avatars_json), "
            + "captured_name = IF(VALUES(combat_power) >= combat_power, VALUES(captured_name), captured_name), "
            + "captured_server = IF(VALUES(combat_power) >= combat_power, VALUES(captured_server), captured_server), "
            + "captured_class = IF(VALUES(combat_power) >= combat_power, VALUES(captured_class), captured_class), "
            + "captured_character_level = IF(VALUES(combat_power) >= combat_power, VALUES(captured_character_level), captured_character_level), "
            + "captured_item_level = IF(VALUES(combat_power) >= combat_power, VALUES(captured_item_level), captured_item_level), "
            + "captured_at = IF(VALUES(combat_power) >= combat_power, VALUES(captured_at), captured_at), "
            + "combat_power = GREATEST(combat_power, VALUES(combat_power))",
            nativeQuery = true)
    int upsertBestSnapshot(
            @Param("characterUid") String characterUid,
            @Param("combatPower") BigDecimal combatPower,
            @Param("engravingsJson") String engravingsJson,
            @Param("gemsJson") String gemsJson,
            @Param("arkPassiveJson") String arkPassiveJson,
            @Param("arkGridJson") String arkGridJson,
            @Param("skillsJson") String skillsJson,
            @Param("avatarsJson") String avatarsJson,
            @Param("capturedName") String capturedName,
            @Param("capturedServer") String capturedServer,
            @Param("capturedClass") String capturedClass,
            @Param("capturedCharacterLevel") Integer capturedCharacterLevel,
            @Param("capturedItemLevel") String capturedItemLevel,
            @Param("capturedAt") LocalDateTime capturedAt);
}
