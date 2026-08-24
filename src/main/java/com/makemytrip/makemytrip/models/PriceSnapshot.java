package com.makemytrip.makemytrip.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "priceSnapshots")
public class PriceSnapshot {
    @Id
    private String id;
    private String flightId;
    private double basePrice;
    private double effectivePrice;
    private double multiplier;
    private String reason;
    private String timestamp;

    public String getId() { return id; }
    public String getFlightId() { return flightId; }
    public void setFlightId(String f) { this.flightId = f; }
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double b) { this.basePrice = b; }
    public double getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(double e) { this.effectivePrice = e; }
    public double getMultiplier() { return multiplier; }
    public void setMultiplier(double m) { this.multiplier = m; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String t) { this.timestamp = t; }
}
