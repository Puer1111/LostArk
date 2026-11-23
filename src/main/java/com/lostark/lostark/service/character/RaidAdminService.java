package com.lostark.lostark.service.character;

import com.lostark.lostark.model.dto.character.RaidDto;
import com.lostark.lostark.model.entity.character.Raid;
import com.lostark.lostark.model.entity.character.RaidGate;
import com.lostark.lostark.model.entity.character.RaidGateDifficulty;
import com.lostark.lostark.model.repository.character.RaidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RaidAdminService {
    private final RaidRepository raidRepository;

    @Transactional(readOnly = true)
    public List<RaidDto> getAllRaids() {
        return raidRepository.findAllByOrderByRaidCategoryAscRaidNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public RaidDto saveRaid(RaidDto dto) {
        validate(dto);
        Raid raid = dto.id() == null ? new Raid() : raidRepository.findById(dto.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raid not found."));
        raid.setRaidName(dto.raidName().trim());
        raid.setRaidCategory(dto.raidCategory());
        raid.setMemberCount(dto.memberCount());
        List<RaidGate> gates = dto.gates().stream().map(gateDto -> {
            RaidGate gate = new RaidGate();
            gate.setGateNumber(gateDto.gateNumber());
            gate.setBossName(gateDto.bossName().trim());
            List<RaidGateDifficulty> difficulties = gateDto.difficulties().stream().map(difficultyDto -> {
                RaidGateDifficulty difficulty = new RaidGateDifficulty();
                difficulty.setDifficulty(difficultyDto.difficulty());
                difficulty.setRaidHp(parseHp(difficultyDto.raidHp()));
                difficulty.setRaidGate(gate);
                return difficulty;
            }).toList();
            gate.replaceDifficulties(difficulties);
            gate.setRaid(raid);
            return gate;
        }).toList();
        raid.replaceGates(gates);
        return toDto(raidRepository.save(raid));
    }

    @Transactional
    public void deleteRaid(Long id) {
        if (!raidRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Raid not found.");
        raidRepository.deleteById(id);
    }

    private void validate(RaidDto dto) {
        if (dto == null || dto.raidName() == null || dto.raidName().isBlank()
                || dto.raidCategory() == null
                || dto.memberCount() == null || dto.memberCount() < 1
                || dto.gates() == null || dto.gates().isEmpty()) {
            throw badRequest("레이드 이름, 지정된 종류, 인원과 관문 정보를 입력해 주세요.");
        }
        for (RaidDto.GateDto gate : dto.gates()) {
            if (gate.gateNumber() == null || gate.gateNumber() < 1 || gate.bossName() == null || gate.bossName().isBlank()
                    || gate.difficulties() == null || gate.difficulties().isEmpty()) throw badRequest("관문 정보를 확인해 주세요.");
            for (RaidDto.DifficultyDto difficulty : gate.difficulties()) {
                if (difficulty.difficulty() == null) throw badRequest("난이도를 선택해 주세요.");
                parseHp(difficulty.raidHp());
            }
        }
    }

    private Long parseHp(String value) {
        try {
            long hp = Long.parseLong(value);
            if (hp < 0) throw badRequest("HP는 0 이상이어야 합니다.");
            return hp;
        } catch (NumberFormatException | NullPointerException exception) {
            throw badRequest("HP는 0 이상의 정수로 입력해 주세요.");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private RaidDto toDto(Raid raid) {
        return new RaidDto(raid.getId(), raid.getRaidName(), raid.getRaidCategory(), raid.getMemberCount(),
                (raid.getMemberCount() + 3) / 4, raid.getGates().stream()
                .map(gate -> new RaidDto.GateDto(gate.getId(), gate.getGateNumber(), gate.getBossName(),
                        gate.getDifficulties().stream().map(difficulty -> new RaidDto.DifficultyDto(difficulty.getId(), difficulty.getDifficulty(), String.valueOf(difficulty.getRaidHp()))).toList()))
                .toList());
    }
}
