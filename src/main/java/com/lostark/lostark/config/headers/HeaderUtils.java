package com.lostark.lostark.config.headers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class HeaderUtils {

    @Value("${LOSTARK_API_KEY}")
    private String apiKey;

    public HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "bearer " + apiKey);
        return headers;
    }
}
