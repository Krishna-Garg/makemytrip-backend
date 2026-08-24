package com.makemytrip.makemytrip.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "flight")
public class Flight {
    @Id
    private String _id;
    private String flightName;
    private String from;
    private String to;
    private String departureTime;
    private String arrivalTime;
    private double price;
    private int availableSeats;

    private String status = "ON-TIME";
    private String statusReason;
    private String estimatedDeparture;
    private int boardingMinutes = 45;
    private int delayMinutes = 0;

    private boolean isTemplate = false;
    private List<String> recurringDays = new ArrayList<>();
    private String templateId;
    private String baseTime;

    private List<StatusUpdate> statusHistory = new ArrayList<>();

    private String aircraftModel;
    private List<Seat> seats = new ArrayList<>();
 
    public static class Seat {
        private String seatNumber;   // e.g. "1A", "12C"
        private String seatClass;    // "ECONOMY" | "PREMIUM" | "BUSINESS"
        private String colorHex;     // for frontend coloring
        private double price;        // per-seat price
        // AVAILABLE | BOOKED | LOCKED
        private String status = "AVAILABLE";
        private String lockedByUserId;
 
        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String s) { this.seatNumber = s; }
        public String getSeatClass() { return seatClass; }
        public void setSeatClass(String s) { this.seatClass = s; }
        public String getColorHex() { return colorHex; }
        public void setColorHex(String c) { this.colorHex = c; }
        public double getPrice() { return price; }
        public void setPrice(double p) { this.price = p; }
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public String getLockedByUserId() { return lockedByUserId; }
        public void setLockedByUserId(String l) { this.lockedByUserId = l; }
    }


    public static class StatusUpdate {
        private String status;
        private String reason;
        private String updatedAt;
        private String updatedBy;
        public String getStatus() {return status;}
        public void setStatus(String s) {this.status = s;}
        public String getReason() {return reason;}
        public void setReason(String r) {this.reason = r;}
        public String getUpdatedAt() {return updatedAt;}
        public void setUpdatedAt(String updatedAt) {this.updatedAt = updatedAt;}
        public String getUpdatedBy() {return updatedBy;}
        public void setUpdatedBy(String updatedBy) {this.updatedBy = updatedBy;}
    }

    // Getters and Setters

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }

    public String getFlightName() {
        return flightName;
    }

    public void setFlightName(String flightName) {
        this.flightName = flightName;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getEstimatedDeparture() { return estimatedDeparture; }
    public void setEstimatedDeparture(String e) { this.estimatedDeparture = e; }
    public int getBoardingMinutes() { return boardingMinutes; }
    public void setBoardingMinutes(int b) { this.boardingMinutes = b; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int d) { this.delayMinutes = d; }
    public boolean isTemplate() { return isTemplate; }
    public void setTemplate(boolean t) { this.isTemplate = t; }
    public List<String> getRecurringDays() { return recurringDays; }
    public void setRecurringDays(List<String> r) { this.recurringDays = r; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String t) { this.templateId = t; }
    public String getBaseTime() { return baseTime; }
    public void setBaseTime(String b) { this.baseTime = b; }
    public List<StatusUpdate> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<StatusUpdate> s) { this.statusHistory = s; }

    public String getAircraftModel() { return aircraftModel; }
    public void setAircraftModel(String a) { this.aircraftModel = a; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> s) { this.seats = s; }


}
