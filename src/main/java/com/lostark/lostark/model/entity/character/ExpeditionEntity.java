package com.lostark.lostark.model.entity.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "expedition")
@Getter
@Setter
@NoArgsConstructor
public class ExpeditionEntity {

    @Id
    @Column(name = "expedition_uid", nullable = false, length = 36)
    private String expeditionUid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;
}
