package com.stockcast.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockPrice {
    private String ticker;
    private double price;
    private LocalDateTime timestamp;
    private double changePercent;

    public String toMessage() {
        return String.format("PRICE|%s|%.2f|%s|%.2f%%", 
            ticker, price, timestamp, changePercent);
    }

    public static StockPrice fromMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length >= 4 && "PRICE".equals(parts[0])) {
            StockPrice stockPrice = new StockPrice();
            stockPrice.setTicker(parts[1]);
            stockPrice.setPrice(Double.parseDouble(parts[2]));
            stockPrice.setTimestamp(LocalDateTime.parse(parts[3]));
            if (parts.length > 4) {
                stockPrice.setChangePercent(Double.parseDouble(parts[4].replace("%", "")));
            }
            return stockPrice;
        }
        return null;
    }
}
