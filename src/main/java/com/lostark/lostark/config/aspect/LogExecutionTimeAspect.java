package com.lostark.lostark.config.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LogExecutionTimeAspect {
    /**
     * 로스트아크 api 응답시간 측정 ( 빠른 요청 시 요청 안되는거 체크 )
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.lostark.lostark.config.aspect.LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        // 실제 메서드 실행
        Object proceed = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - start;

        log.info("[MONITORING] {} executed in {}ms", joinPoint.getSignature().toShortString(), executionTime);
        
        return proceed;
    }
}
