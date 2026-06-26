package com.lostark.lostark.service.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostark.lostark.config.aspect.LogExecutionTime;
import com.lostark.lostark.config.headers.HeaderUtils;
import com.lostark.lostark.model.dto.LostArkCalendar;
import com.lostark.lostark.model.dto.character.CharacterEquipment;
import com.lostark.lostark.model.dto.character.CharacterProfiles;
import com.lostark.lostark.model.dto.character.search.SearchCharacterDTO;
import com.lostark.lostark.model.dto.character.search.SearchExpeditionDTO;
import com.lostark.lostark.model.dto.character.search.SimplifiedCharacterDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@Service
@Slf4j
@RequiredArgsConstructor
public class LostArkServiceImpl implements LostArkService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HeaderUtils headerUtils;
    private final StringRedisTemplate redisTemplate;
    private final org.springframework.beans.factory.ObjectProvider<LostArkService> lostArkServiceProvider;

    private static final String RANKING_KEY = "combat_power_ranking";
    private static final String CHARACTER_METADATA_KEY = "character_metadata";

    private LostArkService getSelf() {
        return lostArkServiceProvider.getIfAvailable();
    }

    /**
     * 개별 캐릭터 프로필 조회 (Redis 캐싱 적용 - profileCache)
     */
    @Override
    @Cacheable(value = "profileCache", key = "#characterName")
    @CircuitBreaker(name = "lostArkCircuitBreaker", fallbackMethod = "fallbackGetCharacterProfile")
    @RateLimiter(name = "lostArkRateLimiter")
    public CharacterProfiles getCharacterProfile(String characterName) {
        log.info("Service.getCharacterProfile.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/armories/characters/")
                 .path("{characterName}/profiles").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            CharacterProfiles profile = objectMapper.readValue(response.getBody(), CharacterProfiles.class);
            
            if (profile != null) {
                updateRanking(profile);
            }
            
            return profile;
        } catch (Exception e) {
            log.warn("Failed to fetch profile for character: {}", characterName);
            // 429 에러 발생 시 예외를 던져 캐싱되지 않도록 함 (다음에 다시 시도할 수 있게)
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("409"))) {
                throw new RuntimeException("API Limit or Conflict for character: " + characterName);
            }
            return null;
        }
    }

    public CharacterProfiles fallbackGetCharacterProfile(String characterName, Throwable t) {
        log.error("Fallback getCharacterProfile activated for character: {}, reason: {}", characterName, t.getMessage());
        return null;
    }

    /**
     * Redis Sorted Set을 이용한 랭킹 업데이트 및 캐릭터 메타데이터 저장
     */
    private void updateRanking(CharacterProfiles profile) {
        try {
            String characterName = profile.getCharacterName();
            if (characterName == null || profile.getCombatPower() == null) return;

            String combatPowerStr = profile.getCombatPower().replace(",", "");
            double combatPower = Double.parseDouble(combatPowerStr);

            // 1. Sorted Set에 전투력 업데이트 (ZSet: 점수 기반 정렬)
            redisTemplate.opsForZSet().add(RANKING_KEY, characterName, combatPower);

            // 2. 캐릭터 메타데이터(이미지, 클래스 등) Hash에 저장 (랭킹 출력용 JSON)
            SimplifiedCharacterDTO metadata = SimplifiedCharacterDTO.builder()
                    .characterName(characterName)
                    .characterImage(profile.getCharacterImage())
                    .characterClassName(profile.getCharacterClassName())
                    .combatPower(profile.getCombatPower())
                    .itemLevel(profile.getItemAvgLevel())
                    .build();

            redisTemplate.opsForHash().put(CHARACTER_METADATA_KEY, characterName, objectMapper.writeValueAsString(metadata));
            log.debug("Ranking updated for character: {}, CombatPower: {}", characterName, combatPower);
        } catch (Exception e) {
            log.warn("Failed to update ranking for character: {}", profile.getCharacterName());
        }
    }

    @Override
    public List<SimplifiedCharacterDTO> getTopRankings() {
        log.info(">>> [비즈니스 로직] 전투력 Top 10 랭킹 조회");
        
        // Redis Sorted Set에서 상위 10명 가져오기 (전투력 높은 순)
        Set<String> topCharacterNames = redisTemplate.opsForZSet().reverseRange(RANKING_KEY, 0, 9);
        
        if (topCharacterNames == null || topCharacterNames.isEmpty()) {
            return Collections.emptyList();
        }

        return topCharacterNames.stream()
                .map(name -> {
                    try {
                        String json = (String) redisTemplate.opsForHash().get(CHARACTER_METADATA_KEY, name);
                        if (json != null) {
                            return objectMapper.readValue(json, SimplifiedCharacterDTO.class);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse metadata for character: {}", name);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @LogExecutionTime
    @Cacheable(value = "expeditionCache", key = "#characterName")
    @CircuitBreaker(name = "lostArkCircuitBreaker", fallbackMethod = "fallbackGetExpedition")
    @RateLimiter(name = "lostArkRateLimiter")
    public SearchExpeditionDTO[] getExpedition(String characterName) {
        log.info("Service.getExpedition.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/characters/")
                .path("{characterName}/siblings").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            SearchExpeditionDTO[] siblings = objectMapper.readValue(response.getBody(), SearchExpeditionDTO[].class);

            if (siblings != null) {
                // 아이템 레벨 기준 정렬하여 상위 캐릭터 우선순위 부여
                List<SearchExpeditionDTO> siblingList = Arrays.asList(siblings);
                siblingList.sort((s1, s2) -> {
                    try {
                        double l1 = Double.parseDouble(s1.getItemAvgLevel().replace(",", ""));
                        double l2 = Double.parseDouble(s2.getItemAvgLevel().replace(",", ""));
                        return Double.compare(l2, l1);
                    } catch (Exception e) { return 0; }
                });

                // Java 21 가상 스레드 Executor를 사용하여 캐릭터 프로필 정보를 동시에 비동기 병렬로 조회
                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    List<CompletableFuture<Void>> futures = siblingList.stream()
                            .map(sibling -> CompletableFuture.runAsync(() -> {
                                try {
                                    // getSelf()를 통해 프록시를 거쳐 호출함으로써 캐시가 작동하도록 함
                                    CharacterProfiles profile = getSelf().getCharacterProfile(sibling.getCharacterName());
                                    if (profile != null) {
                                        sibling.setCharacterImage(profile.getCharacterImage());
                                        sibling.setCombatPower(profile.getCombatPower());
                                    }
                                } catch (Exception e) {
                                    log.warn("Failed to fetch profile in parallel for character: {}, error: {}",
                                            sibling.getCharacterName(), e.getMessage());
                                }
                            }, executor))
                            .collect(Collectors.toList());

                    // 모든 병렬 작업이 완료될 때까지 대기
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                }
            }
            return siblings;
        } catch (Exception e) {
            log.error("Error fetching or parsing expedition for character: {}", characterName, e);
            throw new RuntimeException("원정대 데이터 조회 중 오류 발생", e);
        }
    }

    public SearchExpeditionDTO[] fallbackGetExpedition(String characterName, Throwable t) {
        log.error("Fallback getExpedition activated for character: {}, reason: {}", characterName, t.getMessage());
        return new SearchExpeditionDTO[0];
    }

    @Override
    @LogExecutionTime
    @Cacheable(value = "characterCache", key = "#characterName")
    @CircuitBreaker(name = "lostArkCircuitBreaker", fallbackMethod = "fallbackGetCharacter")
    @RateLimiter(name = "lostArkRateLimiter")
    public SearchCharacterDTO getCharacter(String characterName) {
        log.info("Service.getCharacter.characterName = {}", characterName);
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/armories/characters/")
                .path("{characterName}").encode().buildAndExpand(characterName).toUri();
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            SearchCharacterDTO dto = objectMapper.readValue(response.getBody(), SearchCharacterDTO.class);
            if (dto == null) {
                return new SearchCharacterDTO();
            }
            
            // 프로필 정보가 포함되어 있다면 랭킹 업데이트
            if (dto.getCharacterProfiles() != null) {
                updateRanking(dto.getCharacterProfiles());
            }

            sortGems(dto);
            filterAndSortEquipment(dto);
            return dto;

        } catch (Exception e) {
            log.error("Error fetching or parsing character data for: {}", characterName, e);
            throw new RuntimeException("캐릭터 데이터 조회 중 오류 발생", e);
        }
    }

    public SearchCharacterDTO fallbackGetCharacter(String characterName, Throwable t) {
        log.error("Fallback getCharacter activated for character: {}, reason: {}", characterName, t.getMessage());
        return new SearchCharacterDTO();
    }

    private void sortGems(SearchCharacterDTO dto) {
        if (dto.getCharacterGems() == null || dto.getCharacterGems().getGems() == null) {
            return;
        }
        dto.getCharacterGems().getGems().sort(Comparator.comparing(gem -> {
            if (gem.getTooltip() == null) return 2;
            String effectType = gem.getTooltip().getPrimaryEffectType();
            switch (effectType) {
                case "INCREASE": return 0;
                case "DECREASE": return 1;
                default: return 2;
            }
        }));
    }

    private void filterAndSortEquipment(SearchCharacterDTO dto) {
        if (dto.getCharacterEquipment() == null) {
            return;
        }

        Set<String> desiredTypes = new HashSet<>(Arrays.asList(
                "투구", "어깨", "상의", "하의", "장갑", "무기",
                "목걸이", "귀걸이", "반지", "팔찌", "어빌리티 스톤"
        ));

        List<CharacterEquipment> filteredEquipment = dto.getCharacterEquipment().stream()
                .filter(equip -> desiredTypes.contains(equip.getType()))
                .collect(Collectors.toList());

        List<String> equipmentOrder = Arrays.asList(
                "투구", "어깨", "상의", "하의", "장갑", "무기",
                "목걸이", "귀걸이", "반지", "팔찌", "어빌리티 스톤"
        );
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < equipmentOrder.size(); i++) {
            orderMap.put(equipmentOrder.get(i), i);
        }

        filteredEquipment.sort(Comparator.comparingInt(equip ->
                orderMap.getOrDefault(equip.getType(), Integer.MAX_VALUE)
        ));

        dto.setCharacterEquipment(filteredEquipment);
    }

    @Override
    @LogExecutionTime
    public SimplifiedCharacterDTO getSimplifiedCharacter(String characterName) {
        log.info("Service.getSimplifiedCharacter.characterName = {}", characterName);
        
        // Simplified 조회의 경우도 캐싱된 프로필 정보를 우선적으로 사용하도록 개선 가능
        CharacterProfiles profile = getCharacterProfile(characterName);

        if (profile == null) {
            return null;
        }

        return SimplifiedCharacterDTO.builder()
                .characterImage(profile.getCharacterImage())
                .characterName(profile.getCharacterName())
                .characterClassName(profile.getCharacterClassName())
                .combatPower(profile.getCombatPower())
                .itemLevel(profile.getItemAvgLevel())
                .build();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "profileCache", key = "#characterName"),
            @CacheEvict(value = "expeditionCache", key = "#characterName"),
            @CacheEvict(value = "characterCache", key = "#characterName"),
            @CacheEvict(value = "rankingCache", allEntries = true) // 데이터 갱신 시 랭킹 캐시도 초기화
    })
    public void refreshCharacter(String characterName) {
        log.info("Service.refreshCharacter.characterName = {}", characterName);
    }

    /**
     * 오늘 나타나는 주요 일정(모험 섬, 카오스게이트, 필드보스) 필터링
     * 로스트아크 일일 초기화 시간인 오전 6시를 기준으로 '오늘'의 범위를 정의합니다.
     * @return
     */
    @Override
    public List<LostArkCalendar> getTodayEvents() {
        log.info(">>> [비즈니스 로직] 오전 6시 초기화 기준 오늘 일정 필터링");
        List<LostArkCalendar> allCalendar = getCalendar();
        
        if (allCalendar == null || allCalendar.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 현재 '게임 기준 날짜' 계산 (6시간을 빼서 날짜 판별)
        // 예: 16일 02시 -> 15일로 인식 / 16일 07시 -> 16일로 인식
        LocalDate gameToday = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"))
                .minusHours(6)
                .toLocalDate();

        return allCalendar.stream()
                // 1. 카테고리 필터
                .filter(item -> item.getCategoryName() != null && 
                               (item.getCategoryName().contains("모험 섬") || 
                                item.getCategoryName().contains("카오스게이트") || 
                                item.getCategoryName().contains("필드보스")))
                // 2. 이벤트 시간들도 6시간씩 뒤로 밀어 '게임 오늘' 날짜와 일치하는 일정이 포함된 항목만 필터링
                .filter(item -> item.getStartTimes() != null && 
                               item.getStartTimes().stream()
                                   .anyMatch(t -> t.minusHours(6).toLocalDate().isEqual(gameToday)))
                .map(item -> {
                    // ⚠️ 중요: 캐시된 원본 객체를 수정하지 않기 위해 새 객체를 생성하여 반환합니다 (Deep Copy)
                    
                    // '게임 오늘'에 해당하는 시작 시간만 따로 추출
                    List<LocalDateTime> filteredTimes = item.getStartTimes().stream()
                            .filter(t -> t.minusHours(6).toLocalDate().isEqual(gameToday))
                            .collect(Collectors.toList());

                    // '게임 오늘' 획득 가능한 보상만 따로 추출
                    List<LostArkCalendar.RewardItemLevel> filteredRewards = Collections.emptyList();
                    if (item.getRewardItems() != null) {
                        filteredRewards = item.getRewardItems().stream()
                                .map(level -> {
                                    List<LostArkCalendar.RewardItem> items = level.getItems().stream()
                                            .filter(reward -> reward.getStartTimes() != null && 
                                                             reward.getStartTimes().stream()
                                                                 .anyMatch(t -> t.minusHours(6).toLocalDate().isEqual(gameToday)))
                                            .collect(Collectors.toList());
                                    return new LostArkCalendar.RewardItemLevel(level.getItemLevel(), items);
                                })
                                .filter(level -> !level.getItems().isEmpty())
                                .collect(Collectors.toList());
                    }

                    // 카오스게이트의 경우 지역명이 포함된 이름을 정규화 (예: "일렁이는 악마군단 (베른 북부)" -> "일렁이는 악마군단")
                    String normalizedContentsName = item.getContentsName();
                    if (item.getCategoryName() != null && item.getCategoryName().contains("카오스게이트") && normalizedContentsName != null) {
                        normalizedContentsName = normalizedContentsName.replaceAll("\\s*\\(.*?\\)", "").trim();
                    }

                    // 새로운 객체에 오늘 날짜 정보만 담아서 재구성
                    return LostArkCalendar.builder()
                            .categoryName(item.getCategoryName())
                            .contentsName(normalizedContentsName)
                            .contentsIcon(item.getContentsIcon())
                            .minItemLevel(item.getMinItemLevel())
                            .location(item.getLocation())
                            .startTimes(filteredTimes)
                            .rewardItems(filteredRewards)
                            .build();
                })
                // 중복 제거: 카테고리와 정규화된 이름이 같은 경우 하나만 남김 (LinkedHashMap으로 순서 유지)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                item -> item.getCategoryName() + "|" + item.getContentsName(),
                                item -> item,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));
    }

    @Override
    @Cacheable(value = "calendarCache")
    @CircuitBreaker(name = "lostArkCircuitBreaker", fallbackMethod = "fallbackGetCalendar")
    @RateLimiter(name = "lostArkRateLimiter")
    public List<LostArkCalendar> getCalendar() {
        log.info(">>> [API 호출] 로스트아크 캘린더 데이터 요청 시작");
        
        URI uri = UriComponentsBuilder.fromUriString("https://developer-lostark.game.onstove.com/gamecontents/calendar")
                .build().toUri();
        
        HttpEntity<String> entity = new HttpEntity<>(headerUtils.createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // API는 배열 형태이므로 TypeReference를 사용하여 List로 파싱해야 함
                List<LostArkCalendar> result = objectMapper.readValue(response.getBody(), new TypeReference<List<LostArkCalendar>>() {});
                log.info(">>> [API 성공] 캘린더 데이터 수신 완료. 아이템 개수: {}", (result != null ? result.size() : 0));
                return result != null ? result : Collections.emptyList();
            } else {
                log.warn(">>> [API 경고] 응답은 성공했으나 데이터가 없거나 상태가 이상함. Status: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error(">>> [API 오류] 401 Unauthorized: API 키가 유효하지 않거나 설정되지 않았습니다. 환경 변수를 확인하세요.");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error(">>> [API 오류] 캘린더 데이터 조회 중 오류 발생: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<LostArkCalendar> fallbackGetCalendar(Throwable t) {
        log.error("Fallback getCalendar activated, reason: {}", t.getMessage());
        return Collections.emptyList();
    }
}
