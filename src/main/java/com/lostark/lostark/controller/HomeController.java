package com.lostark.lostark.controller;

import com.lostark.lostark.model.dto.LostArkCalendar;
import com.lostark.lostark.service.api.LostArkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final LostArkService apiService;

    @GetMapping("/")
    public String index(Model model) {
        log.info("HomeController.index called - Requesting Today's Events and Top Rankings");
        
        List<LostArkCalendar> todayEvents = apiService.getTodayEvents();
        List<com.lostark.lostark.model.dto.character.search.SimplifiedCharacterDTO> topRankings = apiService.getTopRankings();

        // 컨트롤러에서 미리 카테고리별로 그룹화 (HTML 에러 방지 및 가독성)
        Map<String, List<LostArkCalendar>> groupedEvents = todayEvents.stream()
                .collect(Collectors.groupingBy(LostArkCalendar::getCategoryName));

        log.info("Events grouped into {} categories, Rankings found: {}", groupedEvents.size(), topRankings.size());
        
        model.addAttribute("eventsMap", groupedEvents);
        model.addAttribute("topRankings", topRankings);
        return "index";
    }
}
