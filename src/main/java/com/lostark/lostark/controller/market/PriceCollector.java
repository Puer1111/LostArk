package com.lostark.lostark.controller.market;

import com.lostark.lostark.model.entity.market.MarketPriceHistory;
import com.lostark.lostark.model.entity.market.MarketPriceSummary;
import com.lostark.lostark.model.market.MarketPriceHistoryRepository;
import com.lostark.lostark.model.market.MarketPriceSummaryRepository;
import com.lostark.lostark.service.market.MarketService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceCollector {

    private final MarketService marketService;
    private final MarketPriceHistoryRepository historyRepository;
    private final MarketPriceSummaryRepository summaryRepository;

    /**
     * 서버 24시간 유지 못할 시 최근 데이터로 로직 수행 메서드
     */
    @PostConstruct
    public void init() {
        log.info("서버 기동: 최근 데이터 기반 누락된 시세 정산 확인 중...");
        List<java.sql.Date> recentDates = historyRepository.findRecentDates();
        
        for (java.sql.Date sqlDate : recentDates) {
            LocalDate targetDate = sqlDate.toLocalDate();
            // 오늘 날짜는 데이터가 계속 쌓이는 중이므로 제외 (원할 경우 포함 가능)
            if (targetDate.equals(LocalDate.now())) continue;

            long count = summaryRepository.countBySummaryDate(targetDate);
            if (count == 0) {
                log.info("{} 날짜의 정산 데이터가 없어 정산을 시작합니다.", targetDate);
                performSummarize(targetDate);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void collectPrices() {
        log.info("시세 데이터 자동 수집 시작...");
        List<String> categories = List.of("Gems", "Enhancement", "Life", "Engravings");
        for (String category : categories) {
            try {
                if ("Gems".equals(category)) {
                    collectGems();
                } else {
                    collectMarketCategory(category);
                }
            } catch (Exception e) {
                log.error("{} 카테고리 수집 중 오류: {}", category, e.getMessage());
            }
        }
        log.info("시세 데이터 자동 수집 완료");
    }

    // 매일 새벽 00:05분 전일 데이터 요약 및 상세 이력 삭제
    @Scheduled(cron = "0 5 0 * * *")
    public void summarizeAndCleanup() {
        log.info("전일 시세 데이터 정산 시작...");
        performSummarize(LocalDate.now().minusDays(1));

        // 3일 이상 된 상세 데이터 삭제 (분석용 여유분 포함)
        historyRepository.deleteByCollectedAtBefore(LocalDateTime.now().minusDays(3));
        log.info("전일 데이터 정산 및 상세 이력 정리 완료");
    }

    private void performSummarize(LocalDate targetDate) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        // 1. 해당 날짜 평균가 계산
        List<Map<String, Object>> averages = historyRepository.getDailyAverages(start, end);
        
        if (averages.isEmpty()) {
            log.info("{} 날짜에 정산할 데이터가 없습니다.", targetDate);
            return;
        }

        // 2. 요약 테이블 저장
        int saveCount = 0;
        for (Map<String, Object> row : averages) {
            try {
                String name = null;
                Double avg = null;
                String category = null;

                // Key 대소문자 방어적 처리
                for (String key : row.keySet()) {
                    if (key.equalsIgnoreCase("itemName")) name = (String) row.get(key);
                    if (key.equalsIgnoreCase("avgPrice")) avg = ((Number) row.get(key)).doubleValue();
                    if (key.equalsIgnoreCase("category")) category = (String) row.get(key);
                }

                if (name != null && avg != null) {
                    summaryRepository.save(MarketPriceSummary.builder()
                            .itemName(name)
                            .avgPrice(avg)
                            .summaryDate(targetDate)
                            .itemCategory(category)
                            .build());
                    saveCount++;
                }
            } catch (Exception e) {
                log.error("요약 데이터 저장 중 오류: {}", e.getMessage());
            }
        }
        log.info("{} 날짜 정산 완료 (총 {}건 요약)", targetDate, saveCount);
    }

    private void collectGems() {
        Object gemsObj = marketService.getGems();
        if (gemsObj instanceof Map) {
            Map<String, Object> gemsMap = (Map<String, Object>) gemsObj;
            List<Map<String, Object>> items = (List<Map<String, Object>>) gemsMap.get("Items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    saveHistory(item, "GEM");
                }
            }
        }
    }

    private void collectMarketCategory(String category) {
        Object resultObj = marketService.getMarketItems(category);
        if (resultObj instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) resultObj;
            List<Map<String, Object>> items = (List<Map<String, Object>>) resultMap.get("Items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    saveHistory(item, category.toUpperCase());
                }
            }
        }
    }

    private void saveHistory(Map<String, Object> item, String category) {
        try {
            String name = (String) item.get("Name");
            Integer price = extractPrice(item, category);
            if (name != null && price != null && price > 0) {
                historyRepository.save(MarketPriceHistory.builder()
                        .itemName(name)
                        .itemPrice(price)
                        .itemCategory(category)
                        .build());
            }
        } catch (Exception ignored) {}
    }

    private Integer extractPrice(Map<String, Object> item, String category) {
        if ("GEM".equals(category)) {
            Map<String, Object> auctionInfo = (Map<String, Object>) item.get("AuctionInfo");
            if (auctionInfo != null && auctionInfo.get("BuyPrice") != null) {
                return ((Number) auctionInfo.get("BuyPrice")).intValue();
            }
        } else {
            if (item.get("CurrentMinPrice") != null) {
                return ((Number) item.get("CurrentMinPrice")).intValue();
            }
        }
        return null;
    }
}
