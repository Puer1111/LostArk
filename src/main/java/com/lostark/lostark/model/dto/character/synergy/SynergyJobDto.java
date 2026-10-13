package com.lostark.lostark.model.dto.character.synergy;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SynergyJobDto {

    private String className;
    private String classGroup; // 전사, 무도가, 헌터, 마법사, 암살자, 스페셜리스트
    
    @Builder.Default
    private List<ClassEngravingDto> classEngravings = new ArrayList<>();
    
    @Builder.Default
    private List<SkillSynergyDto> skills = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassEngravingDto {
        private String engravingName;
        private String role;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkillSynergyDto {
        private String name; // 시너지 종류 (치명타 저항 감소, 피해 증가 등)
        private String priority; // main 등
        
        @Builder.Default
        private List<SynergySourceDto> sources = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SynergySourceDto {
        private String skillName;
        private String skillImg;
        private String skillDescription;
        private String tripodName;
        private String tripodImg;
        private String tripodRate;
        private String tripodDescription;
        private String baseRate;
    }
}
