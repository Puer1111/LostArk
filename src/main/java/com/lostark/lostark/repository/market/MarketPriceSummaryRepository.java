package com.lostark.lostark.repository.market;

import com.lostark.lostark.model.entity.market.MarketPriceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MarketPriceSummaryRepository extends JpaRepository<MarketPriceSummary, Long> {
    
    // 특정 아이템의 특정 날짜 평균가 조회
    Optional<MarketPriceSummary> findByItemNameAndSummaryDate(String itemName, LocalDate summaryDate);
}
