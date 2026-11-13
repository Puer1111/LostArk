package com.lostark.lostark.service.character;

import com.lostark.lostark.model.dto.character.RaidDto;
import com.lostark.lostark.model.entity.character.Raid;
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
        return raidRepository.findAllByOrderByRaidCategoryAscRaidNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RaidDto saveRaid(RaidDto dto) {
        validate(dto);

        Raid raid = dto.id() == null
                ? new Raid()
                : raidRepository.findById(dto.id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raid not found."));

        raid.setRaidName(dto.raidName().trim());
        raid.setRaidCategory(dto.raidCategory().trim());
        raid.setMemberCount(dto.memberCount());
        raid.setPartyCount(dto.partyCount());

        return toDto(raidRepository.save(raid));
    }

    @Transactional
    public void deleteRaid(Long id) {
        if (!raidRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Raid not found.");
        }
        raidRepository.deleteById(id);
    }

    private void validate(RaidDto dto) {
        if (dto.raidName() == null || dto.raidName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Raid name is required.");
        }
        if (dto.raidCategory() == null || dto.raidCategory().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Raid category is required.");
        }
        if (dto.memberCount() == null || dto.memberCount() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member count must be at least 1.");
        }
        if (dto.partyCount() == null || dto.partyCount() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party count must be at least 1.");
        }
    }

    private RaidDto toDto(Raid raid) {
        return new RaidDto(
                raid.getId(),
                raid.getRaidName(),
                raid.getRaidCategory(),
                raid.getMemberCount(),
                raid.getPartyCount()
        );
    }
}
