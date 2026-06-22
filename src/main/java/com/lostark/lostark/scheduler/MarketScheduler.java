package com.lostark.lostark.scheduler;

import com.lostark.lostark.service.market.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class MarketScheduler {

    private final RedissonClient redissonClient;
    private final MarketService marketService;

    /**
     * 매 시간 정각마다 외부 API를 통해 경매장/거래소 최저가 데이터를 수집합니다.
     * 다중 서버 환경에서 락을 획득한 1대의 서버에서만 실행되도록 분산 락(Redisson)을 적용합니다.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void collectMarketPricesJob() {
        String lockKey = "lock:market:collect";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // waitTime = 0: 락이 선점되어 있으면 바로 대기를 포기하여 대기 스레드 누적을 방지
            // leaseTime = 300: 락 유효 시간 5분
            boolean isLocked = lock.tryLock(0, 300, TimeUnit.SECONDS);
            if (isLocked) {
                log.info("Acquired lock [{}]. Starting market price collection job.", lockKey);
                marketService.collectMarketPrices();
            } else {
                log.info("Failed to acquire lock [{}]. Job skipped (already running on other instance).", lockKey);
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to acquire lock [{}]", lockKey, e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Released lock [{}].", lockKey);
            }
        }
    }

    /**
     * 매일 00시 05분에 어제 하루 동안 수집된 가격 정보를 바탕으로 평균 가격 변동 통계를 계산하여 적재합니다.
     * 다중 서버 환경에서 락을 획득한 1대의 서버에서만 실행되도록 분산 락(Redisson)을 적용합니다.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void dailySummaryJob() {
        String lockKey = "lock:market:daily-summary";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // waitTime = 0: 즉시 포기
            // leaseTime = 600: 통계 계산에 최대 10분의 락 타임아웃 보장
            boolean isLocked = lock.tryLock(0, 600, TimeUnit.SECONDS);
            if (isLocked) {
                log.info("Acquired lock [{}]. Starting daily summary job.", lockKey);
                marketService.calculateDailySummary();
            } else {
                log.info("Failed to acquire lock [{}]. Job skipped (already running on other instance).", lockKey);
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while trying to acquire lock [{}]", lockKey, e);
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Released lock [{}].", lockKey);
            }
        }
    }
}
