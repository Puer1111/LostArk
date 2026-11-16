package com.lostark.lostark.service.character;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.model.dto.character.CharacterProfiles;
import com.lostark.lostark.model.dto.character.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.SearchExpeditionDTO;
import com.lostark.lostark.model.entity.character.CharacterBestSnapshot;
import com.lostark.lostark.model.entity.character.CharacterNameHistory;
import com.lostark.lostark.model.entity.character.ExpeditionEntity;
import com.lostark.lostark.model.entity.character.GameCharacter;
import com.lostark.lostark.model.repository.character.CharacterBestSnapshotRepository;
import com.lostark.lostark.model.repository.character.CharacterNameHistoryRepository;
import com.lostark.lostark.model.repository.character.ExpeditionRepository;
import com.lostark.lostark.model.repository.character.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterDataPersistenceService {

    private final ExpeditionRepository expeditionRepository;
    private final GameCharacterRepository characterRepository;
    private final CharacterNameHistoryRepository nameHistoryRepository;
    private final CharacterBestSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveSearchResult(SearchCharacterDTO searchedCharacter,
                                 JsonNode rawApiResponse,
                                 Collection<SearchExpeditionDTO> siblings) {
        if (searchedCharacter == null || searchedCharacter.getCharacterProfiles() == null
                || rawApiResponse == null) {
            return;
        }

        CharacterProfiles profile = searchedCharacter.getCharacterProfiles();
        String characterName = clean(profile.getCharacterName());
        if (characterName == null) {
            return;
        }

        List<SearchExpeditionDTO> roster = normalizeRoster(siblings, profile);
        LocalDateTime now = LocalDateTime.now();
        ExpeditionEntity expedition = resolveExpedition(roster, now);
        expedition.setLastSeenAt(now);
        expeditionRepository.save(expedition);

        List<GameCharacter> members = characterRepository
                .findAllByExpedition_ExpeditionUid(expedition.getExpeditionUid());
        Set<String> matchedCharacterUids = new HashSet<>();
        Map<String, GameCharacter> rosterCharacters = new HashMap<>();

        List<SearchExpeditionDTO> orderedRoster = roster.stream()
                .sorted(Comparator.comparingInt(sibling -> hasExactMember(sibling, members) ? 0 : 1))
                .toList();

        for (SearchExpeditionDTO sibling : orderedRoster) {
            GameCharacter member = resolveCharacterInExpedition(sibling, members, matchedCharacterUids);
            if (member == null) {
                member = newCharacter(expedition, sibling, now);
                members.add(member);
                characterRepository.saveAndFlush(member);
                ensureCurrentName(member, sibling.getServerName(), sibling.getCharacterName(), now);
            } else {
                updateCharacter(member, sibling, now);
                characterRepository.save(member);
                ensureCurrentName(member, sibling.getServerName(), sibling.getCharacterName(), now);
            }

            matchedCharacterUids.add(member.getCharacterUid());
            rosterCharacters.put(rosterKey(sibling.getServerName(), sibling.getCharacterName()), member);
        }

        SearchExpeditionDTO searchedMember = roster.stream()
                .filter(member -> characterName.equals(member.getCharacterName()))
                .findFirst()
                .orElseGet(() -> toSibling(profile));
        GameCharacter gameCharacter = rosterCharacters.get(
                rosterKey(searchedMember.getServerName(), searchedMember.getCharacterName()));
        if (gameCharacter == null) {
            gameCharacter = resolveCharacterInExpedition(searchedMember, members, matchedCharacterUids);
        }
        if (gameCharacter == null) {
            gameCharacter = newCharacter(expedition, searchedMember, now);
            characterRepository.saveAndFlush(gameCharacter);
            ensureCurrentName(gameCharacter, searchedMember.getServerName(), searchedMember.getCharacterName(), now);
        }

        saveBestSnapshot(gameCharacter, profile, rawApiResponse, now);
    }

    private ExpeditionEntity resolveExpedition(List<SearchExpeditionDTO> roster, LocalDateTime now) {
        List<String> names = roster.stream()
                .map(SearchExpeditionDTO::getCharacterName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<GameCharacter> knownCharacters = names.isEmpty()
                ? List.of()
                : characterRepository.findAllByCurrentNameIn(names);

        Map<String, Candidate> candidates = new HashMap<>();
        for (SearchExpeditionDTO sibling : roster) {
            if (clean(sibling.getCharacterName()) == null || clean(sibling.getCharacterClassName()) == null) {
                continue;
            }
            for (GameCharacter known : knownCharacters) {
                if (!sibling.getCharacterName().equals(known.getCurrentName())
                        || !sibling.getCharacterClassName().equals(known.getClassName())) {
                    continue;
                }
                Candidate candidate = candidates.computeIfAbsent(
                        known.getExpedition().getExpeditionUid(), ignored -> new Candidate(known.getExpedition()));
                if (candidate.matchedCharacterUids.add(known.getCharacterUid())) {
                    candidate.score += sibling.getServerName() != null
                            && sibling.getServerName().equals(known.getServerName()) ? 3 : 1;
                    if (samePositiveLevel(sibling.getCharacterLevel(), known.getCharacterLevel())) {
                        candidate.score += 1;
                    }
                }
            }
        }

        int requiredOverlap = roster.size() == 1 ? 1 : 2;
        List<Candidate> eligible = candidates.values().stream()
                .filter(candidate -> candidate.matchedCharacterUids.size() >= requiredOverlap)
                .filter(candidate -> roster.size() != 1 || candidate.score >= 4)
                .sorted(Comparator.comparingInt((Candidate candidate) -> candidate.score).reversed())
                .toList();

        if (!eligible.isEmpty()
                && (eligible.size() == 1 || eligible.get(0).score > eligible.get(1).score)) {
            return eligible.get(0).expedition;
        }

        ExpeditionEntity expedition = new ExpeditionEntity();
        expedition.setExpeditionUid(UUID.randomUUID().toString());
        expedition.setCreatedAt(now);
        expedition.setLastSeenAt(now);
        return expeditionRepository.saveAndFlush(expedition);
    }

    private GameCharacter resolveCharacterInExpedition(SearchExpeditionDTO sibling,
                                                        List<GameCharacter> members,
                                                        Set<String> alreadyMatched) {
        List<GameCharacter> exact = members.stream()
                .filter(member -> !alreadyMatched.contains(member.getCharacterUid()))
                .filter(member -> Objects.equals(member.getCurrentName(), sibling.getCharacterName()))
                .filter(member -> Objects.equals(member.getClassName(), sibling.getCharacterClassName()))
                .toList();
        if (exact.size() == 1) {
            return exact.get(0);
        }

        Integer incomingLevel = parseInteger(sibling.getCharacterLevel());
        if (incomingLevel == null || clean(sibling.getCharacterClassName()) == null) {
            return null;
        }
        List<GameCharacter> possibleRenames = members.stream()
                .filter(member -> !alreadyMatched.contains(member.getCharacterUid()))
                .filter(member -> Objects.equals(member.getClassName(), sibling.getCharacterClassName()))
                .filter(member -> Objects.equals(member.getCharacterLevel(), incomingLevel))
                .toList();
        return possibleRenames.size() == 1 ? possibleRenames.get(0) : null;
    }

    private boolean hasExactMember(SearchExpeditionDTO sibling, List<GameCharacter> members) {
        return members.stream().anyMatch(member ->
                Objects.equals(member.getCurrentName(), sibling.getCharacterName())
                        && Objects.equals(member.getServerName(), sibling.getServerName())
                        && Objects.equals(member.getClassName(), sibling.getCharacterClassName()));
    }

    private void updateCharacter(GameCharacter character, SearchExpeditionDTO sibling, LocalDateTime now) {
        character.setCurrentName(clean(sibling.getCharacterName()));
        character.setServerName(clean(sibling.getServerName()));
        character.setClassName(clean(sibling.getCharacterClassName()));
        character.setCharacterLevel(parseInteger(sibling.getCharacterLevel()));
        character.setItemLevel(clean(sibling.getItemAvgLevel()));
        character.setLastSeenAt(now);
    }

    private GameCharacter newCharacter(ExpeditionEntity expedition,
                                       SearchExpeditionDTO sibling,
                                       LocalDateTime now) {
        GameCharacter character = new GameCharacter();
        character.setCharacterUid(UUID.randomUUID().toString());
        character.setExpedition(expedition);
        character.setCurrentName(clean(sibling.getCharacterName()));
        character.setServerName(clean(sibling.getServerName()));
        character.setClassName(clean(sibling.getCharacterClassName()));
        character.setCharacterLevel(parseInteger(sibling.getCharacterLevel()));
        character.setItemLevel(clean(sibling.getItemAvgLevel()));
        character.setFirstSeenAt(now);
        character.setLastSeenAt(now);
        return character;
    }

    private void ensureCurrentName(GameCharacter character,
                                   String serverName,
                                   String characterName,
                                   LocalDateTime now) {
        String normalizedServer = clean(serverName);
        String normalizedName = clean(characterName);
        if (normalizedServer == null || normalizedName == null) {
            return;
        }

        List<CharacterNameHistory> currentAliases = nameHistoryRepository
                .findAllByCharacter_CharacterUidAndValidToIsNull(character.getCharacterUid());
        boolean sameAlias = currentAliases.stream().anyMatch(alias ->
                normalizedServer.equals(alias.getServerName())
                        && normalizedName.equals(alias.getCharacterName()));
        if (sameAlias) {
            return;
        }

        for (CharacterNameHistory oldAlias : currentAliases) {
            oldAlias.setValidTo(now);
            nameHistoryRepository.save(oldAlias);
        }

        List<CharacterNameHistory> conflictingAliases = nameHistoryRepository
                .findAllByServerNameAndCharacterNameAndValidToIsNull(normalizedServer, normalizedName);
        for (CharacterNameHistory conflict : conflictingAliases) {
            if (character.getCharacterUid().equals(conflict.getCharacter().getCharacterUid())) {
                continue;
            }
            conflict.setValidTo(now);
            nameHistoryRepository.save(conflict);
            GameCharacter previousOwner = conflict.getCharacter();
            if (normalizedName.equals(previousOwner.getCurrentName())
                    && normalizedServer.equals(previousOwner.getServerName())) {
                previousOwner.setCurrentName(null);
                previousOwner.setLastSeenAt(now);
                characterRepository.save(previousOwner);
            }
        }

        startNameHistory(character, normalizedServer, normalizedName, now);
    }

    private void startNameHistory(GameCharacter character,
                                  String serverName,
                                  String characterName,
                                  LocalDateTime now) {
        String normalizedServer = clean(serverName);
        String normalizedName = clean(characterName);
        if (normalizedServer == null || normalizedName == null) {
            return;
        }

        CharacterNameHistory history = new CharacterNameHistory();
        history.setCharacter(character);
        history.setServerName(normalizedServer);
        history.setCharacterName(normalizedName);
        history.setValidFrom(now);
        nameHistoryRepository.save(history);
    }

    private void saveBestSnapshot(GameCharacter character,
                                  CharacterProfiles profile,
                                  JsonNode rawApiResponse,
                                  LocalDateTime now) {
        BigDecimal combatPower = parseCombatPower(profile.getCombatPower());
        if (combatPower == null) {
            log.warn("전투력을 읽을 수 없어 최고 스펙 스냅샷을 저장하지 않습니다: {}", profile.getCharacterName());
            return;
        }

        try {
            snapshotRepository.upsertBestSnapshot(
                    character.getCharacterUid(),
                    combatPower,
                    serializeSection(rawApiResponse, "ArmoryEngraving"),
                    serializeSection(rawApiResponse, "ArmoryGem"),
                    serializeSection(rawApiResponse, "ArkPassive"),
                    serializeSection(rawApiResponse, "ArkGrid"),
                    serializeSection(rawApiResponse, "ArmorySkills"),
                    serializeSection(rawApiResponse, "ArmoryAvatars"),
                    clean(profile.getCharacterName()),
                    clean(profile.getServerName()),
                    clean(profile.getCharacterClassName()),
                    profile.getCharacterLevel(),
                    clean(profile.getItemAvgLevel()),
                    now);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("캐릭터 스펙 JSON 변환에 실패했습니다.", exception);
        }
    }

    private List<SearchExpeditionDTO> normalizeRoster(Collection<SearchExpeditionDTO> siblings,
                                                      CharacterProfiles profile) {
        Map<String, SearchExpeditionDTO> roster = new HashMap<>();
        if (siblings != null) {
            for (SearchExpeditionDTO sibling : siblings) {
                if (sibling != null && clean(sibling.getCharacterName()) != null) {
                    roster.put(rosterKey(sibling.getServerName(), sibling.getCharacterName()), sibling);
                }
            }
        }
        SearchExpeditionDTO searched = toSibling(profile);
        roster.putIfAbsent(rosterKey(searched.getServerName(), searched.getCharacterName()), searched);
        return new ArrayList<>(roster.values());
    }

    private SearchExpeditionDTO toSibling(CharacterProfiles profile) {
        SearchExpeditionDTO sibling = new SearchExpeditionDTO();
        sibling.setCharacterName(clean(profile.getCharacterName()));
        sibling.setServerName(clean(profile.getServerName()));
        sibling.setCharacterClassName(clean(profile.getCharacterClassName()));
        sibling.setCharacterLevel(String.valueOf(profile.getCharacterLevel()));
        sibling.setItemAvgLevel(clean(profile.getItemAvgLevel()));
        sibling.setCombatPower(clean(profile.getCombatPower()));
        return sibling;
    }

    private String serializeSection(JsonNode rawResponse, String fieldName) throws JsonProcessingException {
        JsonNode section = rawResponse.get(fieldName);
        return section == null || section.isNull() ? null : objectMapper.writeValueAsString(section);
    }

    private BigDecimal parseCombatPower(String value) {
        if (clean(value) == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (clean(value) == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean samePositiveLevel(String value, Integer storedLevel) {
        Integer parsed = parseInteger(value);
        return parsed != null && storedLevel != null && parsed.equals(storedLevel);
    }

    private String rosterKey(String serverName, String characterName) {
        return (clean(serverName) == null ? "" : clean(serverName)) + "\u0000"
                + (clean(characterName) == null ? "" : clean(characterName));
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static class Candidate {
        private final ExpeditionEntity expedition;
        private final Set<String> matchedCharacterUids = new HashSet<>();
        private int score;

        private Candidate(ExpeditionEntity expedition) {
            this.expedition = expedition;
        }
    }
}
