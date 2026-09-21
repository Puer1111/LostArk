package com.lostark.lostark.dto.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketPriceSummaryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemName;
    private Double avgPrice;
    private LocalDate summaryDate;
    private String itemCategory;
}
