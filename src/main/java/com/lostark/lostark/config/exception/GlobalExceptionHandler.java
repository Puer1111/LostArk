package com.lostark.lostark.config.exception; // 패키지명 변경

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleRuntimeException(RuntimeException e, Model model) {
        log.error("Unhandled RuntimeException occurred: {}", e.getMessage(), e);
        model.addAttribute("errorMessage", "데이터를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        return "error/errorPage"; // 에러 페이지 뷰 이름
    }

    // 다른 예외 처리기를 추가할 수 있습니다 (예: 특정 예외, 일반 Exception 등)
    // @ExceptionHandler(SpecificException.class)
    // public String handleSpecificException(SpecificException e, Model model) { ... }
}
