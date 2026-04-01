package com.lostark.lostark.controller.market;

import com.lostark.lostark.service.market.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/main")
    public String goIndex(){
        return "market/main";
    }

    /**
     * 카테고리에 따른 거래소 아이템 리스트 조회 (비동기)
     */
    @GetMapping("/api/items/{category}")
    @ResponseBody
    public ResponseEntity<Object> getMarketItems(@PathVariable String category) {
        return ResponseEntity.ok(marketService.getMarketItems(category));
    }

    /**
     * 보석 시세 정보 조회 (비동기 - 경매장 API 테스트)
     */
    @GetMapping("/api/gems")
    @ResponseBody
    public ResponseEntity<Object> getGems() {
        Map<String, Object> body = new HashMap<>();
        body.put("Sort", "BUY_PRICE");
        body.put("CategoryCode", 210000);
        body.put("CharacterClass", "데빌헌터");
        body.put("ItemTier", 4);
        body.put("ItemGrade", "전설");
        body.put("ItemName", "7레벨 겁화");
        body.put("PageNo", 0);
        body.put("SortCondition", "ASC");

        return ResponseEntity.ok(marketService.getAuctionItems(body));
    }
}
