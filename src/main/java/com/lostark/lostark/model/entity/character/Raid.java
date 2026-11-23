package com.lostark.lostark.model.entity.character;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "raid")
@Getter
@Setter
@NoArgsConstructor
public class Raid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = RaidCategoryConverter.class)
    @Column(name = "raid_category", nullable = false, length = 30)
    private RaidCategory raidCategory;

    @Column(name = "raid_name", nullable = false, length = 100)
    private String raidName;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;


    @OneToMany(mappedBy = "raid", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RaidGate> gates = new ArrayList<>();

    public void replaceGates(List<RaidGate> newGates) {
        gates.clear();
        newGates.forEach(gate -> {
            gate.setRaid(this);
            gates.add(gate);
        });
    }
}
