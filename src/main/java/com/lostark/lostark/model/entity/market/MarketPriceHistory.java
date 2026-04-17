package com.lostark.lostark.model.entity.market;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "market_price_history", indexes = {
    @Index(name = "idx_item_collected_at", columnList = "item_name, collected_at")
})
public class MarketPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "priceHistoryNo")
    private Long priceHistoryNo;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_price", nullable = false)
    private Integer itemPrice;

    @Column(name = "item_category", nullable = false, length = 50)
    private String itemCategory; // GEM, LIFE, ENGRAVING, ENHANCEMENT 등

    @CreationTimestamp
    @Column(name = "collected_at", updatable = false, nullable = false)
    private LocalDateTime collectedAt;
}
