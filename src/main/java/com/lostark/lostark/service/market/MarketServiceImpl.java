package com.lostark.lostark.service.market;

import com.lostark.lostark.config.headers.HeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

    private final RestTemplate restTemplate;
    private final HeaderUtils headerUtils;

    /**
     * 거래소 아이템 api 요청 메서드
     * @param category
     * @return
     */
    @Override
    @Cacheable(value = "marketCache", key = "#category")
//    @CacheEvict(value = "marketCache", key = "#category")
    public Object getMarketItems(String category) {
        String url = "https://developer-lostark.game.onstove.com/markets/items";
        List<Integer> categoryCodes = getCategoryCodes(category);

        List<Object> allItems = new ArrayList<>();
        Map<String, Object> lastResponse = null;

        for (int categoryCode : categoryCodes) {
            if (categoryCode == 0) continue;

            int itemsInThisCategory = 0;
            // 각 카테고리별로 최대 5페이지(50개)까지만 가져오기
            for (int pageNo = 1; pageNo <= 5; pageNo++) {
                Map<String, Object> response = fetchMarketPage(url, categoryCode, pageNo);

                if (response == null || !response.containsKey("Items")) break;

                List<Object> items = (List<Object>) response.get("Items");
                if (items == null || items.isEmpty()) break;

                // 강화/재련 관련 카테고리인 경우 필터링 적용
                if (categoryCode == 50010 || categoryCode == 50020) {
                    items = filterEnhancementItems(items, categoryCode);
                }

                // 각 아이템에 CategoryCode 주입
                for (Object item : items) {
                    if (item instanceof Map) {
                        ((Map<String, Object>) item).put("CategoryCode", categoryCode);
                    }
                }

                allItems.addAll(items);
                itemsInThisCategory += items.size();
                lastResponse = response;

                // 해당 카테고리의 전체 데이터 개수 확인
                Object totalCountObj = response.get("TotalCount");
                if (totalCountObj instanceof Integer) {
                    if (itemsInThisCategory >= (int) totalCountObj) break;
                }

                // API 호출 제한 방지를 위한 아주 짧은 지연 시간
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
        }

        if (lastResponse == null) return null;

        // 최종 결과 생성 (응답 구조를 유지하며 Items만 교체)
        Map<String, Object> finalResult = new HashMap<>(lastResponse);
        finalResult.put("Items", allItems);
        finalResult.put("PageNo", 1);
        finalResult.put("PageSize", allItems.size());
        finalResult.put("TotalCount", allItems.size());

        log.info("마켓 데이터 조회 완료: 카테고리={}, 수집된 아이템 수={}", category, allItems.size());
        return finalResult;
    }

    /**
     * 거래소 검색 body 요청
     * @param url
     * @param categoryCode
     * @param pageNo
     * @return
     */
    private Map fetchMarketPage(String url, int categoryCode, int pageNo) {
        Map<String, Object> body = new HashMap<>();
        body.put("CategoryCode", categoryCode);
        body.put("PageNo", pageNo);
        body.put("Sort", "CURRENT_MIN_PRICE");
        body.put("SortCondition", "DESC");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headerUtils.createHeaders());
        try {
            // 응답을 Map으로 받아야 데이터를 다루기 쉽습니다.
            return restTemplate.postForObject(url, entity, Map.class);
        } catch (Exception e) {
            log.error("마켓 API {}페이지 호출 실패: {}", pageNo, e.getMessage());
            return null;
        }
    }

    /**
     * 강화 재료(50010) 카테고리 전용 필터링 메서드
     * @param items
     * @return
     */
//    private List<Object> filterEnhancementItems(List<Object> items) {
//        return items.stream()
//                .filter(item -> {
//                    if (item instanceof Map) {
//                        String name = (String) ((Map<?, ?>) item).get("Name");
//                        return name != null && (name.contains("야금술") || name.contains("재봉술"));
//
//                    }
//                    return false;
//                })
//                .collect(Collectors.toList());
//    }

    /**
     * 거래소 재련 재료 검색 필터 메서드
     * @param items
     * @param categoryCode
     * @return
     */
    private List<Object> filterEnhancementItems(List<Object> items, int categoryCode) {
        List<String> targetKeywords;
        List<String> excludeKeywords;

        if (categoryCode == 50010) {
            // 재련 재료: 돌파석, 강석, 오레하 등
            targetKeywords = List.of("돌파석", "강석","융화","주머니");
            excludeKeywords = List.of("쇠락", "기본", "몽환", "마수", "비늘", "선혈", "조각", "조화", "생명");
        } else if (categoryCode == 50020) {
            // 재련 보조 재료: 야금술, 재봉술, 태양, 용암, 빙하 등
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
    /**
     * 경매장 api 요청 메서드
     * @param body
     * @return
     */
    @Override
    public Object getAuctionItems(Map<String, Object> body) {
        String url = "https://developer-lostark.game.onstove.com/auctions/items";

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headerUtils.createHeaders());

        try {
            return restTemplate.postForObject(url, entity, Object.class);
        } catch (Exception e) {
            log.error("로스트아크 경매장 API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 보석 탭 검색 필터 메서드
     * @return
     */
    @Override
    @Cacheable(value = "gemCache")
    public Object getGems() {
        String[] types = {"겁화", "작열", "멸화", "홍염"};
        int[] tiers = {4, 4, 3, 3};
        
        // 40개의 검색 조건을 생성 (4종류 * 10레벨)
        List<Map<String, Object>> result = IntStream.range(0, 4).boxed()
                .flatMap(typeIdx -> IntStream.rangeClosed(1, 10).mapToObj(level -> {
                    Map<String, Object> target = new HashMap<>();
                    target.put("name", level + "레벨 " + types[typeIdx]);
                    target.put("tier", tiers[typeIdx]);
                    return target;
                }))
                .parallel() // 40개의 API 요청을 병렬로 처리하여 시간 복잡도 최소화
                .map(target -> fetchCheapestGem((String) target.get("name"), (Integer) target.get("tier")))
                .filter(Objects::nonNull)
                .sorted((a, b) -> Long.compare(getBuyPrice(b), getBuyPrice(a))) // 최종 결과는 가격 내림차순
                .collect(Collectors.toList());

        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("Items", result);
        return finalResponse;
    }

    /**
     * 보석 검색 필터 & 경매장에서도 가능 필터
     * @param itemName
     * @param tier
     * @return
     */
    private Map<String, Object> fetchCheapestGem(String itemName, int tier) {
        Map<String, Object> body = new HashMap<>();
        body.put("CategoryCode", 210000);
        body.put("ItemTier", tier);
        body.put("ItemName", itemName);
        body.put("Sort", "BIDSTART_PRICE");
        body.put("SortCondition", "DESC");
        body.put("PageNo", 0);

        Object response = getAuctionItems(body);
        if (response instanceof Map) {
            Map<String, Object> responseMap = (Map<String, Object>) response;
            List<Map<String, Object>> items = (List<Map<String, Object>>) responseMap.get("Items");
            if (items != null && !items.isEmpty()) {
                return items.get(0); // 정렬 조건이 ASC이므로 첫 번째가 최저가
            }
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
//
//    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
//        Set<Object> seen = ConcurrentHashMap.newKeySet();
//        return t -> seen.add(keyExtractor.apply(t));
//    }

    /**
     * 카테고리 Json 데이터 값 메서드
     * @param category
     * @return
     */

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
