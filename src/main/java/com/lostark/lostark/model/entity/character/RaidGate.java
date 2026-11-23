package com.lostark.lostark.model.entity.character;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "raid_gate", uniqueConstraints = @UniqueConstraint(name = "uk_raid_gate_number", columnNames = {"raid_id", "gate_number"}))
@Getter
@Setter
@NoArgsConstructor
public class RaidGate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raid_id", nullable = false)
    private Raid raid;

    @Column(name = "gate_number", nullable = false)
    private Integer gateNumber;

    @Column(name = "boss_name", nullable = false, length = 100)
    private String bossName;

    @OneToMany(mappedBy = "raidGate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RaidGateDifficulty> difficulties = new ArrayList<>();

    public void replaceDifficulties(List<RaidGateDifficulty> newDifficulties) {
        difficulties.clear();
        newDifficulties.forEach(difficulty -> {
            difficulty.setRaidGate(this);
            difficulties.add(difficulty);
        });
    }
}
