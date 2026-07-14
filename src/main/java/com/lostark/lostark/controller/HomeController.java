package com.lostark.lostark.controller;

import com.lostark.lostark.model.dto.LostArkCalendar;
import com.lostark.lostark.service.api.LostArkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.lostark.lostark.model.dto.LostArkNewsDto;
import com.lostark.lostark.model.dto.LostArkEventDto;
import com.lostark.lostark.service.api.chzzk.ChzzkService;
import com.lostark.lostark.model.dto.chzzk.ChzzkResponse;
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
    private final ChzzkService chzzkService;

    @GetMapping("/")
    public String index(Model model) {
        log.info("HomeController.index called - Requesting Today's Events, Recent Searches, News, Events, and Chzzk Lives");
        
        List<LostArkCalendar> todayEvents = apiService.getTodayEvents();
        List<com.lostark.lostark.model.dto.character.search.SimplifiedCharacterDTO> recentCharacters = apiService.getRecentCharacters();
        List<LostArkNewsDto> notices = apiService.getNewsNotices();
        List<LostArkEventDto> events = apiService.getNewsEvents();
        List<ChzzkResponse.LiveDetail> chzzkLives = chzzkService.getLostArkLives();

        // 컨트롤러에서 미리 카테고리별로 그룹화 (HTML 에러 방지 및 가독성)
        Map<String, List<LostArkCalendar>> groupedEvents = todayEvents.stream()
                .collect(Collectors.groupingBy(LostArkCalendar::getCategoryName));

        // 공지와 이벤트는 화면에 콤팩트하게 출력하기 위해 최대 6개 정도로 조절
        List<LostArkNewsDto> limitedNotices = notices.stream().limit(6).collect(Collectors.toList());
        List<LostArkEventDto> limitedEvents = events.stream().limit(6).collect(Collectors.toList());

        log.info("Events grouped into {} categories, Recent Characters: {}, Notices: {}, Events: {}, Chzzk: {}", 
                groupedEvents.size(), recentCharacters.size(), limitedNotices.size(), limitedEvents.size(), chzzkLives.size());
        
        model.addAttribute("eventsMap", groupedEvents);
        model.addAttribute("recentCharacters", recentCharacters);
        model.addAttribute("notices", limitedNotices);
        model.addAttribute("events", limitedEvents);
        model.addAttribute("chzzkLives", chzzkLives);
        return "index";
    }
}
