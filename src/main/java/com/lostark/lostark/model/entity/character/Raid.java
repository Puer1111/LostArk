package com.lostark.lostark.model.entity.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "raid")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Raid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raid_category", nullable = false, length = 50)
    private String raidCategory;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(name = "raid_name", nullable = false, length = 100)
    private String raidName;

    @Column(name = "party_count", nullable = false)
    private Integer partyCount;
}
