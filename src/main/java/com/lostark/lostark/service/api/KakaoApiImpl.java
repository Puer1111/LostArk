package com.lostark.lostark.service.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoApiImpl implements KakaoApi {

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    @Value("${kakao.api.key}")
    private String clientId;

    @Override
    public String getAccessToken(String code) {
        String accessToken = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", "http://127.0.0.1:7777/users/kakao/callback"); // 리다이렉트 URI
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    reqURL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            HashMap<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            accessToken = (String) responseMap.get("access_token");

        } catch (IOException e) {
            log.error("Error while getting Kakao access token", e);
        }
        return accessToken;
    }

    @Override
    public HashMap<String, Object> getUserInfo(String accessToken) {
        HashMap<String, Object> userInfo = new HashMap<>();
        String reqURL = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    reqURL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            userInfo = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });

        } catch (IOException e) {
            log.error("Error while getting Kakao user info", e);
        }

        return userInfo;
    }

    @Override
    public String getAuthorizationCode() {
        String reqURL = "https://kauth.kakao.com/oauth/authorize";
        String redirectUri = "http://127.0.0.1:7777/users/kakao/callback";
        return reqURL + "?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&response_type=code";
    }

    @Override
    public String getLogoutUrl() {
        return UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/logout")
                .queryParam("client_id", clientId)
                .queryParam("logout_redirect_uri", "http://127.0.0.1:7777")
                .build().toUriString();
    }
}
