package com.makemytrip.makemytrip.models;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
public class FlightNotification {
    @Id private String _id;
    private String userId;
    private String flightId;
    private String flightName;
    private String message;       // "Your flight AI101 is now BOARDING"
    private String type;          // STATUS_CHANGE | DELAY | BOARDING
    private boolean read = false;
    private String createdAt;

    // getters/setters omitted for brevity — standard pattern same as other models
    public String getId() { return _id; }
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