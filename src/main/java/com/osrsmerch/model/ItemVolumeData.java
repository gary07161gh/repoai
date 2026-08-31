package com.osrsmerch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemVolumeData {
    private int avgHighPrice;
    private int highPriceVolume;
    private int avgLowPrice;
    private int lowPriceVolume;

    public int getTotalVolume() {
        return highPriceVolume + lowPriceVolume;
    }
}
