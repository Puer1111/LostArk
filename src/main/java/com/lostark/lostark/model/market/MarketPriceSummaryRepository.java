package com.lostark.lostark.model.market;

import com.lostark.lostark.model.entity.market.MarketPriceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MarketPriceSummaryRepository extends JpaRepository<MarketPriceSummary, Long> {
    
    // 특정 아이템의 특정 날짜 평균가 조회
    Optional<MarketPriceSummary> findByItemNameAndSummaryDate(String itemName, LocalDate summaryDate);

    // 추가: 특정 날짜의 정산 데이터 존재 여부 확인용
    long countBySummaryDate(LocalDate summaryDate);

    // 추가: 특정 날짜의 모든 요약 데이터를 한 번에 가져와 메모리에서 비교하기 위함
    java.util.List<MarketPriceSummary> findBySummaryDate(LocalDate summaryDate);
}
