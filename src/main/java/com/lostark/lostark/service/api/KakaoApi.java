package com.lostark.lostark.service.api;

import java.util.HashMap;

public interface KakaoApi {
    /**
     * 액세스 토큰 받기 (2)
     * @param code
     * @return
     */
    String getAccessToken(String code);

    /**
     * 사용자 정보 조회
     * @param accessToken
     * @return
     */
    HashMap<String, Object> getUserInfo(String accessToken);

    /**
     * 인가 코드 받기 (1)
     * @return
     */
    String getAuthorizationCode();

    String getLogoutUrl();
}
