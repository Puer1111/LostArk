package com.lostark.lostark.controller.market;

import com.lostark.lostark.model.entity.market.MarketPriceHistory;
import com.lostark.lostark.model.entity.market.MarketPriceSummary;
import com.lostark.lostark.repository.market.MarketPriceHistoryRepository;
import com.lostark.lostark.repository.market.MarketPriceSummaryRepository;
import com.lostark.lostark.service.market.MarketService;
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
        LocalDateTime start = LocalDateTime.now().minusDays(1).with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().minusDays(1).with(LocalTime.MAX);

        // 1. 전일 평균가 계산
        List<Map<String, Object>> averages = historyRepository.getDailyAverages(start, end);
        
        // 2. 요약 테이블 저장
        for (Map<String, Object> row : averages) {
            summaryRepository.save(MarketPriceSummary.builder()
                    .itemName((String) row.get("itemName"))
                    .avgPrice((Double) row.get("avgPrice"))
                    .summaryDate(LocalDate.now().minusDays(1))
                    .itemCategory((String) row.get("category"))
                    .build());
        }

        // 3. 3일 이상 된 상세 데이터 삭제 (분석용 여유분 포함)
        historyRepository.deleteByCollectedAtBefore(LocalDateTime.now().minusDays(3));
        log.info("전일 데이터 정산 및 상세 이력 정리 완료 (총 {}건 요약)", averages.size());
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
