package com.makemytrip.makemytrip.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "reviews")
public class Review {

    @Id
    private String _id;

    @Indexed
    private String targetId;

    private String targetType;

    private String userId;
    private String userFullName;

    private int rating;
    private String reviewText;
    private String photoUrl;

    private String createdAt;
    private boolean flagged = false;
    private String flagReason;

    private List<Reply> replies = new ArrayList<>();

    private int helpfulCount = 0;

    //GETTERS AND SETTERS
    public String getId() {return _id;}

    public String getTargetId() {return targetId;}
    public void setTargetId(String targetId) {this.targetId = targetId;}

    public String getTargetType() {return targetType;}
    public void setTargetType(String targetType) {this.targetType = targetType;}

    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}

    public String getUserFullName() {return userFullName;}
    public void setUserFullName(String userFullName) {this.userFullName = userFullName;}

    public int getRating() {return rating;}
    public void setRating(int rating) {this.rating = rating;}

    public String getReviewText() {return reviewText;}
    public void setReviewText(String reviewText) {this.reviewText = reviewText;}

    public String getPhotoUrl() {return photoUrl;}
    public void setPhotoUrl(String photoUrl) {this.photoUrl = photoUrl;}

    public String getCreatedAt() {return createdAt;}
    public void setCreatedAt(String createdAt) {this.createdAt = createdAt;}

    public boolean isFlagged() {return flagged;}
    public void setFlagged(boolean flagged) {this.flagged = flagged;}

    public String getFlagReason() {return flagReason;}
    public void setFlagReason(String flagReason) {this.flagReason = flagReason;}

    public List<Reply> getReplies() {return replies;}
    public void setReplies(List<Reply> replies) {this.replies = replies;}

    public int getHelpfulCount() {return helpfulCount;}
    public void setHelpfulCount(int helpfulCount) {this.helpfulCount = helpfulCount;}


    public static class Reply {
        private String userId;
        private String userFullName;
        private String text;
        private String createdAt;

        public String getUserId() {return userId;}
        public void setUserId(String userId) {this.userId = userId;}

        public String getUserFullName() {return userFullName;}
        public void setUserFullName(String userFullName) {this.userFullName = userFullName;}

        public String getText() {return text;}
        public void setText(String text) {this.text = text;}

        public String getCreatedAt() {return createdAt;}

        public void setCreatedAt(String createdAt) {this.createdAt = createdAt;}
    }
}
