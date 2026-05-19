package com.lostark.lostark.service.market;

import com.lostark.lostark.config.headers.HeaderUtils;
import com.lostark.lostark.model.entity.market.MarketPriceSummary;
import com.lostark.lostark.model.market.MarketPriceHistoryRepository;
import com.lostark.lostark.model.market.MarketPriceSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    public Object getMarketItems(String category) {
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

    @Override
    @Cacheable(value = "gemCache")
    public Object getGems() {
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
}
