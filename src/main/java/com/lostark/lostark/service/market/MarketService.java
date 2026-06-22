package com.lostark.lostark.service.market;

import java.util.Map;

public interface MarketService {
    Object getMarketItems(String category);
    Object getAuctionItems(Map<String, Object> body);
    Object getGems();
    
    // 아이템 시세 시그널(변동률 등) 조회
    Map<String, Object> getPriceSignal(String itemName, Integer currentPrice);

    // 정기 시세 데이터 수집 스케줄러를 위한 메소드
    void collectMarketPrices();

    // 일별 요약 시세 데이터 집계 스케줄러를 위한 메소드
    void calculateDailySummary();
}
