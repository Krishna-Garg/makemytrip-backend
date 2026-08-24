package com.makemytrip.makemytrip.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "recommendationFeedback")
public class RecommendationFeedback {
    @Id
    private String id;
    private String userId;
    private String targetId;       // flightId or hotelId
    private String targetType;     // "FLIGHT" | "HOTEL"
    private String feedback;       // "HELPFUL" | "IRRELEVANT"
    private String createdAt;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String t) { this.targetId = t; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String t) { this.targetType = t; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String f) { this.feedback = f; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String c) { this.createdAt = c; }
}
