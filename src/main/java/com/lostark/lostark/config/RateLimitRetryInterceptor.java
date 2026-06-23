package com.lostark.lostark.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RateLimitRetryInterceptor implements ClientHttpRequestInterceptor {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 300;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long backoffMs = INITIAL_BACKOFF_MS;
        ClientHttpResponse response = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            response = execution.execute(request, body);

            // HTTP Status 429 (Too Many Requests) 체크
            if (response.getStatusCode().value() == 429) {
                log.warn("API 429 Too Many Requests detected for URI: {}. Attempt {} of {}. Retrying in {}ms...",
                        request.getURI(), attempt, MAX_ATTEMPTS, backoffMs);

                if (attempt == MAX_ATTEMPTS) {
                    return response; // 최대 재시도 횟수에 도달하면 에러 응답 그대로 반환
                }

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("API request retry interrupted", ie);
                }
                backoffMs *= 2; // 지수 백오프
            } else {
                return response; // 429가 아니면 즉시 정상 반환
            }
        }
        return response;
    }
}
