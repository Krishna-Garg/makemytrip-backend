package com.makemytrip.makemytrip.models;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.ArrayList;
@Document(collection = "users")
public class Users {
    @Id
    private String _id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String role;
    private String phoneNumber;
    private List<Booking> bookings = new ArrayList<>();

    private String savedSeatPreference;  // "WINDOW" | "AISLE" | "MIDDLE"
    private String savedRoomPreference;  // "STANDARD" | "DELUXE" | "SUITE"


    public String getFirstName() {return firstName;}
    public String getId() { return _id; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPassword() {return password;}
    public String getEmail() {return email;}
    public String getRole() {return role;}
    public void setPassword(String password) {this.password = password;}
    public void setRole(String role) {this.role = role;}
    public List<Booking> getBookings(){return bookings;}
    public void setBookings(List<Booking> bookings){this.bookings=bookings;}

    public String getSavedSeatPreference() { return savedSeatPreference; }
    public void setSavedSeatPreference(String s) { this.savedSeatPreference = s; }
    public String getSavedRoomPreference() { return savedRoomPreference; }
    public void setSavedRoomPreference(String s) { this.savedRoomPreference = s; }

    public static class Booking{
        private String type;
        private String bookingId;
        private String date;
        private int quantity;
        private double totalPrice;

        //For cancellation of a flight
        private String bookingStatus = "CONFIRMED";
        private String cancellationReason;
        private String cancelledAt;
        private String departureTime;
        private double refundAmount;
        private String refundStatus;
        private String refundRequestedAt;
        private String refundCompletedAt;
        private String refundEta;
        private List<String> selectedSeats = new ArrayList<>();
        private String selectedRoomType;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getBookingId() { return bookingId; }
        public void setBookingId(String bookingId) { this.bookingId = bookingId; }
        public String getDate() { return date; }
        public void setDate(String date) {this.date = date;}
        public int getQuantity() {return quantity;}
        public void setQuantity(int quantity) {this.quantity = quantity;}
        public double getTotalPrice() {return totalPrice;}
        public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

        // Getters and Setters for cancellationRefund
        public String getBookingStatus(){ return bookingStatus; }
        public void setBookingStatus( String bookingStatus){ this.bookingStatus = bookingStatus; }
        public String getCancellationReason(){ return cancellationReason; }
        public void setCancellationReason(String cancellationReason){ this.cancellationReason = cancellationReason; }
        public String getCancelledAt(){ return cancelledAt; }
        public void setCancelledAt(String cancelledAt){ this.cancelledAt = cancelledAt; }
        public String getDepartureTime(){ return departureTime; }
        public void setDepartureTime(String departureTime){ this.departureTime = departureTime; }
        public String getRefundStatus(){ return refundStatus; }
        public void setRefundStatus(String refundStatus){ this.refundStatus = refundStatus; }
        public double getRefundAmount(){ return refundAmount; }
        public void setRefundAmount(double refundAmount){ this.refundAmount = refundAmount; }
        public String getRefundRequestedAt() { return refundRequestedAt; }
        public void setRefundRequestedAt(String refundRequestedAt) { this.refundRequestedAt = refundRequestedAt; }
        public String getRefundCompletedAt() { return refundCompletedAt; }
        public void setRefundCompletedAt(String refundCompletedAt) { this.refundCompletedAt = refundCompletedAt; }
        public String getRefundEta() { return refundEta; }
        public void setRefundEta(String refundEta) { this.refundEta = refundEta; }
        public List<String> getSelectedSeats() { return selectedSeats; }
        public void setSelectedSeats(List<String> s) { this.selectedSeats = s; }
        public String getSelectedRoomType() { return selectedRoomType; }
        public void setSelectedRoomType(String s) { this.selectedRoomType = s; }
    }
}
