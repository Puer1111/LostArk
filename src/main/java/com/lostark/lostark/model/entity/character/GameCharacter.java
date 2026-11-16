package com.lostark.lostark.model.entity.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_character")
@Getter
@Setter
@NoArgsConstructor
public class GameCharacter {

    @Id
    @Column(name = "character_uid", nullable = false, length = 36)
    private String characterUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expedition_uid", nullable = false)
    private ExpeditionEntity expedition;

    @Column(name = "current_name", length = 50)
    private String currentName;

    @Column(name = "server_name", length = 50)
    private String serverName;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "character_level")
    private Integer characterLevel;

    @Column(name = "item_level", length = 32)
    private String itemLevel;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;
}
