package com.lostark.lostark.config;

import com.lostark.lostark.model.entity.character.Raid;
import com.lostark.lostark.model.repository.character.RaidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RaidDataInitializer implements CommandLineRunner {

    private final RaidRepository raidRepository;

    @Override
    public void run(String... args) {
        if (raidRepository.count() > 0) {
            return;
        }

        // 새 정규화 구조의 레이드·관문·난이도 데이터는 관리자 화면에서 등록합니다.
    }
}
