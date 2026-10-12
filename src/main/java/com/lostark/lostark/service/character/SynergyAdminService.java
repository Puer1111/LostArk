package com.lostark.lostark.service.character;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.model.dto.character.synergy.SynergyJobDto;
import com.lostark.lostark.model.entity.character.SynergyJob;
import com.lostark.lostark.model.repository.character.SynergyJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynergyAdminService {

    private final SynergyJobRepository synergyJobRepository;
    private final ObjectMapper objectMapper;

    /**
     * 전체 직업 시너지 목록 조회 (클라이언트 시뮬레이터 및 어드민 공용)
     */
    @Transactional(readOnly = true)
    public List<SynergyJobDto> getAllSynergies() {
        List<SynergyJob> entities = synergyJobRepository.findAll();
        List<SynergyJobDto> list = new ArrayList<>();

        for (SynergyJob entity : entities) {
            try {
                SynergyJobDto dto = objectMapper.readValue(entity.getDataJson(), SynergyJobDto.class);
                dto.setClassName(entity.getClassName());
                dto.setClassGroup(entity.getClassGroup());
                list.add(dto);
            } catch (Exception e) {
                log.error("Failed to parse synergy JSON for class: {}", entity.getClassName(), e);
            }
        }
        return list;
    }

    /**
     * 특정 직업 시너지 조회
     */
    @Transactional(readOnly = true)
    public Optional<SynergyJobDto> getSynergyByClassName(String className) {
        return synergyJobRepository.findByClassName(className).map(entity -> {
            try {
                SynergyJobDto dto = objectMapper.readValue(entity.getDataJson(), SynergyJobDto.class);
                dto.setClassName(entity.getClassName());
                dto.setClassGroup(entity.getClassGroup());
                return dto;
            } catch (Exception e) {
                log.error("Failed to parse synergy JSON for class: {}", entity.getClassName(), e);
                return null;
            }
        });
    }

    /**
     * 직업 시너지 등록 / 수정 (Upsert)
     */
    @Transactional
    public SynergyJobDto saveOrUpdateSynergy(SynergyJobDto dto) {
        if (dto.getClassName() == null || dto.getClassName().trim().isEmpty()) {
            throw new IllegalArgumentException("클래스명(className)은 필수입니다.");
        }

        try {
            String json = objectMapper.writeValueAsString(dto);
            SynergyJob entity = synergyJobRepository.findByClassName(dto.getClassName().trim())
                    .orElse(SynergyJob.builder()
                            .className(dto.getClassName().trim())
                            .build());

            entity.setClassGroup(dto.getClassGroup());
            entity.setDataJson(json);

            synergyJobRepository.save(entity);
            return dto;
        } catch (Exception e) {
            log.error("Failed to serialize or save synergy for class: {}", dto.getClassName(), e);
            throw new RuntimeException("시너지 데이터 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 직업 시너지 삭제
     */
    @Transactional
    public void deleteSynergy(String className) {
        synergyJobRepository.findByClassName(className).ifPresent(synergyJobRepository::delete);
    }
}
