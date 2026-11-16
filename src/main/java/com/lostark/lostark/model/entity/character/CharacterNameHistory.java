package com.lostark.lostark.model.entity.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "character_name_history", indexes = {
        @Index(name = "idx_character_alias_lookup", columnList = "server_name, character_name, valid_to")
})
@Getter
@Setter
@NoArgsConstructor
public class CharacterNameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_uid", nullable = false)
    private GameCharacter character;

    @Column(name = "character_name", nullable = false, length = 50)
    private String characterName;

    @Column(name = "server_name", nullable = false, length = 50)
    private String serverName;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;
}
