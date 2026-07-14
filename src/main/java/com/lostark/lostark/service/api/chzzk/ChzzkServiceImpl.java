package com.lostark.lostark.service.api.chzzk;

import com.lostark.lostark.model.dto.chzzk.ChzzkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChzzkServiceImpl implements ChzzkService {

    private final RestTemplate restTemplate;

    @Value("${chzzk.client-id}")
    private String clientId;

    @Value("${chzzk.client-secret}")
    private String clientSecret;
    
    // 로스트아크 카테고리 ID 및 비공식 서비스 API URL
    private static final String LOSTARK_CATEGORY_ID = "Lost_Ark";
    private static final String CHZZK_LIVES_API_URL = "https://api.chzzk.naver.com/service/v2/categories/GAME/" + LOSTARK_CATEGORY_ID + "/lives";

    @Override
    @Cacheable(value = "chzzkLivesCache")
    public List<ChzzkResponse.LiveDetail> getLostArkLives() {
        log.info(">>> [치지직 비공식 API 호출] 로스트아크 생방송 목록 요청");

        // 비공식 API를 사용하지만 환경 변수 설정 경고는 로깅 처리만 유지
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.warn(">>> [치지직 경고] Client-Id 또는 Client-Secret 설정이 비어있습니다. 공식 API 사용 시에는 필요하지만 비공식 API는 계속 동작합니다.");
        }
        
        // 비공식 카테고리별 라이브 API URL 호출
        URI uri = UriComponentsBuilder.fromUriString(CHZZK_LIVES_API_URL)
                .queryParam("size", 20)
                .build().toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        // 봇 탐지 방지용 User-Agent 설정
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ChzzkResponse> response = restTemplate.exchange(uri, HttpMethod.GET, entity, ChzzkResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ChzzkResponse res = response.getBody();
                if (res.getContent() != null && res.getContent().getData() != null) {
                    List<ChzzkResponse.LiveDetail> lives = res.getContent().getData();
                    log.info(">>> [치지직 API 성공] 조회된 로스트아크 방송 개수: {}", lives.size());
                    return lives;
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error(">>> [치지직 API 오류] 방송 목록 조회 중 오류 발생: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
