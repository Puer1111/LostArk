package com.lostark.lostark.repository.market;

import com.lostark.lostark.model.entity.market.MarketPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface MarketPriceHistoryRepository extends JpaRepository<MarketPriceHistory, Long> {

    Optional<MarketPriceHistory> findTopByItemNameOrderByCollectedAtDesc(String itemName);

    @Query("SELECT AVG(m.itemPrice) FROM MarketPriceHistory m " +
           "WHERE m.itemName = :itemName " +
           "AND m.collectedAt BETWEEN :start AND :end")
    Double getAveragePrice(@Param("itemName") String itemName, 
                           @Param("start") LocalDateTime start, 
                           @Param("end") LocalDateTime end);

    // 정산을 위해 모든 아이템의 평균가를 그룹화하여 조회
    @Query("SELECT m.itemName as itemName, AVG(m.itemPrice) as avgPrice, MAX(m.itemCategory) as category " +
           "FROM MarketPriceHistory m " +
           "WHERE m.collectedAt BETWEEN :start AND :end " +
           "GROUP BY m.itemName")
    List<Map<String, Object>> getDailyAverages(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 오래된 상세 이력 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM MarketPriceHistory m WHERE m.collectedAt < :threshold")
    void deleteByCollectedAtBefore(@Param("threshold") LocalDateTime threshold);
}
