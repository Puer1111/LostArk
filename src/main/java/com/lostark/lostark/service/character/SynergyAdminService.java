package com.lostark.lostark.service.character;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.model.dto.character.synergy.SynergyJobDto;
import com.lostark.lostark.model.entity.character.SynergyJob;
import com.lostark.lostark.model.repository.character.SynergyJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
     * 전체 활성 직업 시너지 목록 조회 (클라이언트 시뮬레이터 및 어드민 공용 - Soft Delete 제외)
     */
    @Cacheable(value = "synergyCache")
    @Transactional(readOnly = true)
    public List<SynergyJobDto> getAllSynergies() {
        List<SynergyJob> entities = synergyJobRepository.findAllByIsDeletedFalse();
        return convertEntitiesToDtos(entities);
    }

    /**
     * 삭제된 직업(휴지통) 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SynergyJobDto> getDeletedSynergies() {
        List<SynergyJob> entities = synergyJobRepository.findAllByIsDeletedTrue();
        return convertEntitiesToDtos(entities);
    }

    private List<SynergyJobDto> convertEntitiesToDtos(List<SynergyJob> entities) {
        List<SynergyJobDto> list = new ArrayList<>();
        for (SynergyJob entity : entities) {
            try {
                SynergyJobDto dto = objectMapper.readValue(entity.getDataJson(), SynergyJobDto.class);
                dto.setClassName(entity.getClassName());
                dto.setClassGroup(entity.getClassGroup());
                dto.setIsDeleted(entity.isDeleted());
                if (entity.getDeletedAt() != null) {
                    dto.setDeletedAt(entity.getDeletedAt().toString());
                }
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
                dto.setIsDeleted(entity.isDeleted());
                if (entity.getDeletedAt() != null) {
                    dto.setDeletedAt(entity.getDeletedAt().toString());
                }
                return dto;
            } catch (Exception e) {
                log.error("Failed to parse synergy JSON for class: {}", entity.getClassName(), e);
                return null;
            }
        });
    }

    /**
     * 직업 시너지 등록 / 수정 (Upsert)
     * 이전에 삭제(Soft Delete)된 직업인 경우 다시 활성화(restore)
     */
    @CacheEvict(value = "synergyCache", allEntries = true)
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
            entity.restore(); // 저장 시 활성화 상태로 복원

            synergyJobRepository.save(entity);
            return dto;
        } catch (Exception e) {
            log.error("Failed to serialize or save synergy for class: {}", dto.getClassName(), e);
            throw new RuntimeException("시너지 데이터 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 직업 시너지 논리 삭제 (Soft Delete)
     */
    @CacheEvict(value = "synergyCache", allEntries = true)
    @Transactional
    public void deleteSynergy(String className) {
        synergyJobRepository.findByClassName(className).ifPresent(entity -> {
            entity.markDeleted();
            synergyJobRepository.save(entity);
            log.info("Soft deleted synergy job: {}", className);
        });
    }

    /**
     * 삭제된 직업 시너지 복원 (Restore)
     */
    @CacheEvict(value = "synergyCache", allEntries = true)
    @Transactional
    public void restoreSynergy(String className) {
        synergyJobRepository.findByClassName(className).ifPresent(entity -> {
            entity.restore();
            synergyJobRepository.save(entity);
            log.info("Restored synergy job: {}", className);
        });
    }
}
