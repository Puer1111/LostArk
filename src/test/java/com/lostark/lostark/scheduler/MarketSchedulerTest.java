package com.lostark.lostark.scheduler;

import com.lostark.lostark.service.market.MarketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketSchedulerTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private MarketService marketService;

    @InjectMocks
    private MarketScheduler marketScheduler;

    @Test
    @DisplayName("다중 스레드에서 동시에 스케줄러 메소드 진입 시, Redis 분산 락에 의해 단 1번만 실행되는지 동시성 테스트")
    void collectMarketPricesJobConcurrencyTest() throws Exception {
        // given
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        // 락 획득 상태를 시뮬레이션하기 위한 상태 변수
        AtomicBoolean isLocked = new AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicReference<Thread> lockingThread = new java.util.concurrent.atomic.AtomicReference<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        RLock mockLock = mock(RLock.class);

        // RedissonClient가 lock 객체를 반환하도록 모킹
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);

        // 1등 스레드가 락을 쥔 채 비즈니스 로직을 돌게 하기 위해 200ms 딜레이 주입
        doAnswer(invocation -> {
            Thread.sleep(200);
            return null;
        }).when(marketService).collectMarketPrices();

        // tryLock 호출 시, 동시성 상황을 모킹
        // 하나의 스레드가 tryLock에 성공하면 isLocked가 true가 되고, 락 소유 스레드로 등록됨
        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
            boolean acquired = isLocked.compareAndSet(false, true);
            if (acquired) {
                lockingThread.set(Thread.currentThread());
            }
            return acquired;
        });

        // unlock 호출 시, 락 해제 상태로 되돌림
        doAnswer(invocation -> {
            isLocked.set(false);
            lockingThread.set(null);
            return null;
        }).when(mockLock).unlock();

        // 락 해제 권한 체크 모킹 (현재 호출한 스레드가 실제 락을 쥐고 있는가)
        when(mockLock.isHeldByCurrentThread()).thenAnswer(invocation -> 
            Thread.currentThread().equals(lockingThread.get())
        );

        // when: 다중 스레드에서 동시에 스케줄러 작업 실행
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.await(); // 모든 스레드가 준비될 때까지 대기
                    marketScheduler.collectMarketPricesJob();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        latch.countDown(); // 대기 중인 모든 스레드를 동시에 출발시킴
        finishLatch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
        executorService.shutdown();

        // then
        // 1. 에러 없이 모든 스레드가 작업을 완료했는지 확인
        assertThat(failCount.get()).isZero();
        assertThat(successCount.get()).isEqualTo(threadCount);

        // 2. 비즈니스 로직(marketService.collectMarketPrices())이 전체 스레드 중 정확히 단 1번만 실행되었는지 검증
        verify(marketService, times(1)).collectMarketPrices();
        
        // 3. 락이 정확히 1번 해제되었는지 검증
        verify(mockLock, times(1)).unlock();
    }
    
    @Test
    @DisplayName("일별 요약 시세 데이터 적재 시 분산 락에 의해 단 1번만 실행되는지 동시성 테스트")
    void dailySummaryJobConcurrencyTest() throws Exception {
        // given
        int threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicBoolean isLocked = new AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicReference<Thread> lockingThread = new java.util.concurrent.atomic.AtomicReference<>();
        RLock mockLock = mock(RLock.class);

        when(redissonClient.getLock("lock:market:daily-summary")).thenReturn(mockLock);

        // 1등 스레드가 정산 로직을 수행하는 동안 200ms 대기
        doAnswer(invocation -> {
            Thread.sleep(200);
            return null;
        }).when(marketService).calculateDailySummary();

        when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
            boolean acquired = isLocked.compareAndSet(false, true);
            if (acquired) {
                lockingThread.set(Thread.currentThread());
            }
            return acquired;
        });
        
        doAnswer(invocation -> {
            isLocked.set(false);
            lockingThread.set(null);
            return null;
        }).when(mockLock).unlock();
        
        when(mockLock.isHeldByCurrentThread()).thenAnswer(invocation -> 
            Thread.currentThread().equals(lockingThread.get())
        );

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    marketScheduler.dailySummaryJob();
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        latch.countDown();
        finishLatch.await();
        executorService.shutdown();

        // then
        verify(marketService, times(1)).calculateDailySummary();
        verify(mockLock, times(1)).unlock();
    }
}
