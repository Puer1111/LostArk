package com.lostark.lostark.model.entity.market;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "market_price_summary", indexes = {
    @Index(name = "idx_summary_item_date", columnList = "item_name, summary_date")
})
public class MarketPriceSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summaryNo")
    private Long summaryNo;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "avg_price", nullable = false)
    private Double avgPrice;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "item_category", length = 50)
    private String itemCategory;
}
