package com.lostark.lostark.model.entity.character;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "synergy_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SynergyJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_name", nullable = false, unique = true, length = 50)
    private String className;

    @Column(name = "class_group", length = 50)
    private String classGroup; // 전사, 무도가, 헌터, 마법사, 암살자, 스페셜리스트

    @Lob
    @Column(name = "data_json", columnDefinition = "LONGTEXT", nullable = false)
    private String dataJson; // 각인, 스킬, 시너지 구조가 포함된 JSON

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
