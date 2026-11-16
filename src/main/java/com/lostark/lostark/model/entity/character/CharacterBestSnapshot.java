package com.lostark.lostark.model.entity.character;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "character_best_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class CharacterBestSnapshot {

    @Id
    @Column(name = "character_uid", nullable = false, length = 36)
    private String characterUid;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_uid", nullable = false)
    private GameCharacter character;

    @Column(name = "combat_power", nullable = false, precision = 12, scale = 2)
    private BigDecimal combatPower;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "engravings_json")
    private JsonNode engravingsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gems_json")
    private JsonNode gemsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ark_passive_json")
    private JsonNode arkPassiveJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ark_grid_json")
    private JsonNode arkGridJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills_json")
    private JsonNode skillsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avatars_json")
    private JsonNode avatarsJson;

    @Column(name = "captured_name", nullable = false, length = 50)
    private String capturedName;

    @Column(name = "captured_server", length = 50)
    private String capturedServer;

    @Column(name = "captured_class", length = 50)
    private String capturedClass;

    @Column(name = "captured_character_level")
    private Integer capturedCharacterLevel;

    @Column(name = "captured_item_level", length = 32)
    private String capturedItemLevel;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;
}
