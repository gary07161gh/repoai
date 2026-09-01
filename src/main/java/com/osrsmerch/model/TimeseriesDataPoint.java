package com.osrsmerch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeseriesDataPoint {
    private long timestamp;
    private int avgHighPrice;
    private int avgLowPrice;
    private int highPriceVolume;
    private int lowPriceVolume;
}
