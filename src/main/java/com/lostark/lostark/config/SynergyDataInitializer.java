package com.lostark.lostark.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.model.dto.character.synergy.SynergyJobDto;
import com.lostark.lostark.model.repository.character.SynergyJobRepository;
import com.lostark.lostark.service.character.SynergyAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SynergyDataInitializer implements ApplicationRunner {

    private final SynergyJobRepository synergyJobRepository;
    private final SynergyAdminService synergyAdminService;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        long count = synergyJobRepository.count();
        if (count > 0) {
            log.info("[SynergyDataInitializer] 기존 직업 시너지 데이터가 존재합니다. (총 {}건) 초기 적재를 건너뜁니다.", count);
            return;
        }

        log.info("[SynergyDataInitializer] 직업 시너지 데이터가 존재하지 않아 기본 시드 데이터를 적재합니다.");
        loadJsonAndSave("merged_25_jobs.json");
        loadJsonAndSave("extra_3_jobs.json");
        log.info("[SynergyDataInitializer] 기본 시드 데이터 적재 완료. (총 {}건)", synergyJobRepository.count());
    }

    private void loadJsonAndSave(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("[SynergyDataInitializer] 시드 리소스 파일을 찾을 수 없습니다: {}", resourcePath);
                return;
            }

            List<SynergyJobDto> list;
            try (InputStream is = resource.getInputStream()) {
                list = objectMapper.readValue(is, new TypeReference<List<SynergyJobDto>>() {});
            }

            if (list != null) {
                for (SynergyJobDto dto : list) {
                    synergyAdminService.saveOrUpdateSynergy(dto);
                }
                log.info("[SynergyDataInitializer] {} 로부터 {}건의 직업 시너지를 적재했습니다.", resourcePath, list.size());
            }
        } catch (Exception e) {
            log.error("[SynergyDataInitializer] 시드 데이터 적재 실패 ({})", resourcePath, e);
        }
    }
}
