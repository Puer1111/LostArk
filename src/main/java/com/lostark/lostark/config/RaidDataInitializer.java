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

        List<Raid> raids = List.of(
                raid("\uBC1C\uD0C4", "\uAD70\uB2E8\uC7A5", 8, 2),
                raid("\uBE44\uC544\uD0A4\uC2A4", "\uAD70\uB2E8\uC7A5", 8, 2),
                raid("\uCFE0\uD06C\uC138\uC774\uD2BC", "\uAD70\uB2E8\uC7A5", 4, 1),
                raid("\uC544\uBE0C\uB810\uC288\uB4DC", "\uAD70\uB2E8\uC7A5", 8, 2),
                raid("\uCE74\uC81C\uB85C\uC2A4 1\uB9C9", "\uCE74\uC81C\uB85C\uC2A4", 8, 2),
                raid("\uCE74\uC81C\uB85C\uC2A4 2\uB9C9", "\uCE74\uC81C\uB85C\uC2A4", 8, 2),
                raid("\uCE74\uC81C\uB85C\uC2A4 3\uB9C9", "\uCE74\uC81C\uB85C\uC2A4", 8, 2),
                raid("\uCE74\uC81C\uB85C\uC2A4 4\uB9C9", "\uCE74\uC81C\uB85C\uC2A4", 8, 2),
                raid("\uCE74\uC81C\uB85C\uC2A4 \uC885\uB9C9", "\uCE74\uC81C\uB85C\uC2A4", 8, 2),
                raid("\uCE74\uC591\uAC94", "\uC5B4\uBE44\uC2A4", 4, 1),
                raid("\uC0C1\uC544\uD0D1", "\uC5B4\uBE44\uC2A4", 4, 1),
                raid("\uC9C0\uD3C9\uC758 \uC131\uB2F9", "\uC5B4\uBE44\uC2A4", 4, 1),
                raid("\uC138\uB974\uCE74", "\uADF8\uB9BC\uC790", 4, 1)
        );

        raidRepository.saveAll(raids);
    }

    private Raid raid(String name, String category, int maxPlayers, int partyCount) {
        return Raid.builder()
                .raidName(name)
                .raidCategory(category)
                .memberCount(maxPlayers)
                .partyCount(partyCount)
                .build();
    }
}
