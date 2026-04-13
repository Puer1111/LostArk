package com.lostark.lostark.service.market;

import java.util.Map;

public interface MarketService {
    Object getMarketItems(String category);
    Object getAuctionItems(Map<String, Object> body);
    Object getGems();
}
