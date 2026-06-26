package com.lostark.lostark.service.market;

import com.lostark.lostark.config.headers.HeaderUtils;
import com.lostark.lostark.model.entity.market.MarketPriceHistory;
import com.lostark.lostark.model.entity.market.MarketPriceSummary;
import com.lostark.lostark.model.market.MarketPriceHistoryRepository;
import com.lostark.lostark.model.market.MarketPriceSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

    private final RestTemplate restTemplate;
    private final HeaderUtils headerUtils;
    private final MarketPriceHistoryRepository historyRepository;
    private final MarketPriceSummaryRepository summaryRepository;

    @Override
    @Cacheable(value = "marketCache", key = "#category")
    @CircuitBreaker(name = "lostArkCircuitBreaker", fallbackMethod = "fallbackGetMarketItems")
    @RateLimiter(name = "lostArkRateLimiter")
    public Object getMarketItems(String category) {
        return getMarketItemsInternal(category);
    }

    public Object fallbackGetMarketItems(String category, Throwable t) {
        log.error("Fallback getMarketItems activated for category: {}, reason: {}", category, t.getMessage());
        return null;
    }

    public Object getMarketItemsInternal(String category) {
        String url = "https://developer-lostark.game.onstove.com/markets/items";
        List<Integer> categoryCodes = getCategoryCodes(category);

        List<Object> allItems = new ArrayList<>();
        Map<String, Object> lastResponse = null;

        for (int categoryCode : categoryCodes) {
            if (categoryCode == 0) continue;

            int itemsProcessedFromApi = 0; // API에서 실제로 가져온 원본 아이템 수
            for (int pageNo = 1; pageNo <= 15; pageNo++) { // 최대 150개 품목까지 조회 확대
                Map<String, Object> response = fetchMarketPage(url, categoryCode, pageNo);

                if (response == null || !response.containsKey("Items")) break;

                List<Object> rawItems = (List<Object>) response.get("Items");
                if (rawItems == null || rawItems.isEmpty()) break;

                itemsProcessedFromApi += rawItems.size();
                List<Object> filteredItems = rawItems;

                // 각인서(40000)인 경우 유물 등급만 확실하게 필터링
                if (categoryCode == 40000) {
                    filteredItems = rawItems.stream()
                            .filter(item -> item instanceof Map && "유물".equals(((Map<?, ?>) item).get("Grade")))
                            .collect(Collectors.toList());
                }

                if (categoryCode == 50010 || categoryCode == 50020) {
                    filteredItems = filterEnhancementItems(rawItems, categoryCode);
                }

                for (Object item : filteredItems) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        itemMap.put("CategoryCode", categoryCode);
                        try {
                            String name = (String) itemMap.get("Name");
                            Integer price = ((Number) itemMap.get("CurrentMinPrice")).intValue();
                            itemMap.put("Signal", getPriceSignal(name, price));
                        } catch (Exception e) {
                            log.error("시그널 주입 중 오류: {}", e.getMessage());
                        }
                    }
                }

                allItems.addAll(filteredItems);
                lastResponse = response;

                Object totalCountObj = response.get("TotalCount");
                if (totalCountObj instanceof Integer) {
                    // API에서 준 전체 개수만큼 다 긁었으면 종료
                    if (itemsProcessedFromApi >= (int) totalCountObj) break;
                }
                
                // 마지막 페이지가 아니면 딜레이 (Rate Limit 방지)
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }

        }

        if (lastResponse == null) return null;
        Map<String, Object> finalResult = new HashMap<>(lastResponse);
        finalResult.put("Items", allItems);
        return finalResult;
    }

    private Map fetchMarketPage(String url, int categoryCode, int pageNo) {
        Map<String, Object> body = new HashMap<>();
        body.put("CategoryCode", categoryCode);
        if (categoryCode == 40000) {
            body.put("ItemGrades", "유물");
        }
        body.put("Sort", "CURRENT_MIN_PRICE");
        body.put("SortCondition", "DESC");
        body.put("PageNo", pageNo);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headerUtils.createHeaders());
        try {
            return restTemplate.postForObject(url, entity, Map.class);
        } catch (Exception e) {
            log.error("마켓 API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<Object> filterEnhancementItems(List<Object> items, int categoryCode) {
        List<String> targetKeywords;
        List<String> excludeKeywords;
        if (categoryCode == 50010) {
            targetKeywords = List.of("돌파석", "강석","융화","주머니");
            excludeKeywords = List.of("쇠락", "기본", "몽환", "마수", "비늘", "선혈", "조각", "조화", "생명");
        } else if (categoryCode == 50020) {
            targetKeywords = List.of("야금술", "재봉술", "태양", "용암", "빙하");
            excludeKeywords = List.of("쇠락", "기본", "몽환", "마수", "비늘", "선혈", "조각", "조화", "생명");
        } else {
            return items;
        }
        return items.stream()
                .filter(item -> {
                    if (item instanceof Map) {
                        String name = (String) ((Map<?, ?>) item).get("Name");
                        if (name == null) return false;
                        boolean hasTarget = targetKeywords.stream().anyMatch(name::contains);
                        boolean hasExclude = excludeKeywords.stream().anyMatch(name::contains);
                        return hasTarget && !hasExclude;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    @Override
    @CircuitBreaker(name = "lostArkCircuitBreaker", fallbackMethod = "fallbackGetAuctionItems")
    @RateLimiter(name = "lostArkRateLimiter")
    public Object getAuctionItems(Map<String, Object> body) {
        String url = "https://developer-lostark.game.onstove.com/auctions/items";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headerUtils.createHeaders());
        try {
            return restTemplate.postForObject(url, entity, Object.class);
        } catch (Exception e) {
            log.error("경매장 API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    public Object fallbackGetAuctionItems(Map<String, Object> body, Throwable t) {
        log.error("Fallback getAuctionItems activated, reason: {}", t.getMessage());
        return null;
    }

    @Override
    @Cacheable(value = "gemCache")
    @CircuitBreaker(name = "lostArkCircuitBreaker", fallbackMethod = "fallbackGetGems")
    @RateLimiter(name = "lostArkRateLimiter")
    public Object getGems() {
        return getGemsInternal();
    }

    public Object fallbackGetGems(Throwable t) {
        log.error("Fallback getGems activated, reason: {}", t.getMessage());
        return null;
    }

    public Object getGemsInternal() {
        String[] types = {"겁화", "작열", "멸화", "홍염"};
        int[] tiers = {4, 4, 3, 3};
        List<Map<String, Object>> result = IntStream.range(0, 4).boxed()
                .flatMap(typeIdx -> IntStream.rangeClosed(1, 10).mapToObj(level -> {
                    Map<String, Object> target = new HashMap<>();
                    target.put("name", level + "레벨 " + types[typeIdx]);
                    target.put("tier", tiers[typeIdx]);
                    return target;
                }))
                .parallel()
                .map(target -> fetchCheapestGem((String) target.get("name"), (Integer) target.get("tier")))
                .filter(Objects::nonNull)
                .peek(item -> {
                    try {
                        String name = (String) item.get("Name");
                        Map<String, Object> auctionInfo = (Map<String, Object>) item.get("AuctionInfo");
                        if (auctionInfo != null && auctionInfo.get("BuyPrice") != null) {
                            Integer price = ((Number) auctionInfo.get("BuyPrice")).intValue();
                            item.put("Signal", getPriceSignal(name, price));
                        }
                    } catch (Exception ignored) {}
                })
                .sorted((a, b) -> Long.compare(getBuyPrice(b), getBuyPrice(a)))
                .collect(Collectors.toList());
        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("Items", result);
        return finalResponse;
    }

    private Map<String, Object> fetchCheapestGem(String itemName, int tier) {
        Map<String, Object> body = new HashMap<>();
        body.put("CategoryCode", 210000);
        body.put("ItemTier", tier);
        body.put("ItemName", itemName);
        body.put("Sort", "BUY_PRICE");
        body.put("SortCondition", "ASC");
        body.put("PageNo", 0);
        Object response = getAuctionItems(body);
        if (response instanceof Map) {
            Map<String, Object> responseMap = (Map<String, Object>) response;
            List<Map<String, Object>> items = (List<Map<String, Object>>) responseMap.get("Items");
            if (items != null && !items.isEmpty()) return items.get(0);
        }
        return null;
    }

    private long getBuyPrice(Map<String, Object> item) {
        Map<String, Object> auctionInfo = (Map<String, Object>) item.get("AuctionInfo");
        if (auctionInfo != null && auctionInfo.get("BuyPrice") != null) {
            return ((Number) auctionInfo.get("BuyPrice")).longValue();
        }
        return 0L;
    }

    @Override
    public Map<String, Object> getPriceSignal(String itemName, Integer currentPrice) {
        Map<String, Object> result = new HashMap<>();
        Double avgPrice = null;

        // 1. 먼저 요약 테이블(Summary)에서 어제 평균가 조회 시도
        Optional<MarketPriceSummary> yesterdaySummary = summaryRepository.findByItemNameAndSummaryDate(itemName, LocalDate.now().minusDays(1));
        
        if (yesterdaySummary.isPresent()) {
            avgPrice = yesterdaySummary.get().getAvgPrice();
        } else {
            // 2. 요약 데이터가 없으면 최근 24시간 이력에서 평균 계산 (더 넓은 범위로 보정)
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().minusSeconds(5);
            avgPrice = historyRepository.getAveragePrice(itemName, start, end);
        }
        
        if (avgPrice == null || avgPrice == 0) {
            result.put("status", "STABLE");
            result.put("message", "데이터 수집 중");
            result.put("diffRate", 0.0);
            return result;
        }

        double diffRate = ((currentPrice - avgPrice) / avgPrice) * 100;
        result.put("diffRate", Math.round(diffRate * 10) / 10.0);

        // 시그널 기준 보정: -2% ~ +2% 사이는 안정권으로 판단
        if (diffRate <= -5) {
            result.put("status", "DOWN");
            result.put("message", "급락! 특가");
        } else if (diffRate <= -2) {
            result.put("status", "DOWN");
            result.put("message", "최근보다 저렴");
        } else if (diffRate >= 5) {
            result.put("status", "UP");
            result.put("message", "상승세");
        } else if (diffRate >= 2) {
            result.put("status", "UP");
            result.put("message", "최근보다 비쌈");
        } else {
            result.put("status", "STABLE");
            result.put("message", "가격 안정");
        }
        return result;
    }

    private List<Integer> getCategoryCodes(String category) {
        switch (category) {
            case "Gems": return List.of(210000);
            case "Life": return List.of(90000);
            case "Engravings": return List.of(40000);
            case "Enhancement": return List.of(50010,50020,51100,230000);
            default: return List.of(0);
        }
    }

    @Override
    public void collectMarketPrices() {
        log.info("Starting market price collection (Non-transactional API calls)...");
        LocalDateTime collectedAt = LocalDateTime.now();

        // 1. 일반 아이템 수집 (Life, Engravings, Enhancement)
        List<String> categories = List.of("Life", "Engravings", "Enhancement");
        for (String category : categories) {
            try {
                Object resultObj = getMarketItemsInternal(category);
                if (resultObj instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    List<Object> items = (List<Object>) resultMap.get("Items");
                    if (items != null) {
                        List<MarketPriceHistory> histories = new ArrayList<>();
                        for (Object itemObj : items) {
                            if (itemObj instanceof Map) {
                                Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                                String name = (String) itemMap.get("Name");
                                Integer price = ((Number) itemMap.get("CurrentMinPrice")).intValue();
                                histories.add(MarketPriceHistory.builder()
                                        .itemName(name)
                                        .itemPrice(price)
                                        .itemCategory(category.toUpperCase())
                                        .collectedAt(collectedAt)
                                        .build());
                            }
                        }
                        if (!histories.isEmpty()) {
                            // saveAll은 SimpleJpaRepository 내부에서 @Transactional 처리됨
                            historyRepository.saveAll(histories);
                            log.info("Saved {} price history records for category {}", histories.size(), category);
                        }
                    }
                }
                // API Rate Limit 방지
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("Error collecting prices for category {}: {}", category, e.getMessage(), e);
            }
        }

        // 2. 보석 아이템 수집 (Gems)
        try {
            Object gemsObj = getGemsInternal();
            if (gemsObj instanceof Map) {
                Map<String, Object> gemsMap = (Map<String, Object>) gemsObj;
                List<Object> items = (List<Object>) gemsMap.get("Items");
                if (items != null) {
                    List<MarketPriceHistory> histories = new ArrayList<>();
                    for (Object itemObj : items) {
                        if (itemObj instanceof Map) {
                            Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                            String name = (String) itemMap.get("Name");
                            Map<String, Object> auctionInfo = (Map<String, Object>) itemMap.get("AuctionInfo");
                            if (auctionInfo != null && auctionInfo.get("BuyPrice") != null) {
                                Integer price = ((Number) auctionInfo.get("BuyPrice")).intValue();
                                histories.add(MarketPriceHistory.builder()
                                        .itemName(name)
                                        .itemPrice(price)
                                        .itemCategory("GEMS")
                                        .collectedAt(collectedAt)
                                        .build());
                            }
                        }
                    }
                    if (!histories.isEmpty()) {
                        historyRepository.saveAll(histories);
                        log.info("Saved {} price history records for category GEMS", histories.size());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error collecting prices for GEMS: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void calculateDailySummary() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.atTime(23, 59, 59);

        log.info("Starting daily summary calculation for {} ({} ~ {})", yesterday, start, end);

        List<Map<String, Object>> dailyAverages = historyRepository.getDailyAverages(start, end);
        if (dailyAverages == null || dailyAverages.isEmpty()) {
            log.warn("No price history records found for daily summary on {}", yesterday);
            return;
        }

        // 중복 체크 및 조회 쿼리 N+1 방지를 위해 어제 날짜에 등록된 요약 정보를 한 번에 읽음
        List<MarketPriceSummary> existingSummaries = summaryRepository.findBySummaryDate(yesterday);
        Set<String> existingItemNames = existingSummaries.stream()
                .map(MarketPriceSummary::getItemName)
                .collect(Collectors.toSet());

        List<MarketPriceSummary> summariesToSave = new ArrayList<>();
        for (Map<String, Object> avgMap : dailyAverages) {
            String itemName = (String) avgMap.get("itemName");
            Double avgPrice = (Double) avgMap.get("avgPrice");
            String category = (String) avgMap.get("category");

            if (itemName == null || avgPrice == null) continue;

            // 메모리 상에서 이미 집계된 아이템인지 확인 (N+1 쿼리 최적화)
            if (existingItemNames.contains(itemName)) {
                log.info("Summary already exists for item: {} on {}", itemName, yesterday);
                continue;
            }

            summariesToSave.add(MarketPriceSummary.builder()
                    .itemName(itemName)
                    .avgPrice(avgPrice)
                    .summaryDate(yesterday)
                    .itemCategory(category)
                    .build());
        }

        if (!summariesToSave.isEmpty()) {
            summaryRepository.saveAll(summariesToSave);
            log.info("Saved {} new daily summary records for {}", summariesToSave.size(), yesterday);
        }
    }
}
