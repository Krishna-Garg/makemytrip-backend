// ═══════════════════════════════════════════════════════════
// models/FlightNotification.java
// ═══════════════════════════════════════════════════════════
package com.makemytrip.makemytrip.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "flightNotifications")
public class FlightNotification {
    @Id
    private String id;
    private String userId;
    private String flightId;
    private String flightName;
    private String message;
    private String type;        // STATUS_CHANGE | DELAY | BOARDING | DEPARTED
    private boolean read = false;
    private String createdAt;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public String getFlightId() { return flightId; }
    public void setFlightId(String f) { this.flightId = f; }
    public String getFlightName() { return flightName; }
    public void setFlightName(String f) { this.flightName = f; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public boolean isRead() { return read; }
    public void setRead(boolean r) { this.read = r; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String c) { this.createdAt = c; }
}
