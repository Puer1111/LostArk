package com.lostark.lostark.controller.market;

import com.lostark.lostark.service.market.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
}
