package com.lostark.lostark.service.market;

import com.lostark.lostark.config.headers.HeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

    private final RestTemplate restTemplate;
    private final HeaderUtils headerUtils;

    @Override
    public Object getMarketItems(String category) {
        String url = "https://developer-lostark.game.onstove.com/markets/items";

        // 카테고리에 따른 코드 매핑
        int categoryCode = getCategoryCode(category);

        // 요청 바디 설정
        Map<String, Object> body = new HashMap<>();
        body.put("CategoryCode", categoryCode);
        body.put("ItemGrade", "유물");
        body.put("PageNo", 1);
        body.put("Sort", "CURRENT_MIN_PRICE");
        body.put("SortCondition", "DESC");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headerUtils.createHeaders());

        try {
            return restTemplate.postForObject(url, entity, Object.class);
        } catch (Exception e) {
            log.error("로스트아크 거래소 API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private int getCategoryCode(String category) {
        switch (category) {
            case "Gems": return 210000;       // 보석
            case "Life": return 90000;       // 생활
            case "Engravings": return 40000; // 각인서
            case "Enhancement": return 50010; // 재련 재료
            default: return 0;
        }
    }
}
