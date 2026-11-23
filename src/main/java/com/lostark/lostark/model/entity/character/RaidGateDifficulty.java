package com.lostark.lostark.model.entity.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "raid_gate_difficulty", uniqueConstraints = @UniqueConstraint(name = "uk_gate_difficulty", columnNames = {"raid_gate_id", "difficulty"}))
@Getter
@Setter
@NoArgsConstructor
public class RaidGateDifficulty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raid_gate_id", nullable = false)
    private RaidGate raidGate;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 30)
    private RaidDifficulty difficulty;

    @Column(name = "raid_hp", nullable = false)
    private Long raidHp;
}
