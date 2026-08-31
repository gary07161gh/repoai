package com.osrsmerch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemMapping {
    private int id;
    private String name;
    private String examine;
    private boolean members;
    private int lowalch;
    private int highalch;
    private int limit; // 4-hour buy limit
    private int value; // Store base price
    private String icon;
}
