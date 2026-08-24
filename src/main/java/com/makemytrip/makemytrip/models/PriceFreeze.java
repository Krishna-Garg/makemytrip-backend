package com.makemytrip.makemytrip.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "priceFreezes")
public class PriceFreeze {
    @Id
    private String id;
    private String userId;
    private String flightId;
    private double frozenPrice;
    private String expiresAt;
    private boolean used = false;
    private String createdAt;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public String getFlightId() { return flightId; }
    public void setFlightId(String f) { this.flightId = f; }
    public double getFrozenPrice() { return frozenPrice; }
    public void setFrozenPrice(double f) { this.frozenPrice = f; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String e) { this.expiresAt = e; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean u) { this.used = u; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String c) { this.createdAt = c; }
}
