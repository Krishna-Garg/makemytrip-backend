package com.makemytrip.makemytrip.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
public class Users {
    @Id private String _id;
    private String firstName;
    private String lastName;
    private String email;

    @JsonIgnore
    private String password;

    private String role;
    private String phoneNumber;
    private List<Booking> bookings = new ArrayList<>();
    private String savedSeatPreference;
    private String savedRoomPreference;

    public String getId() { return _id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }
    public String getSavedSeatPreference() { return savedSeatPreference; }
    public void setSavedSeatPreference(String s) { this.savedSeatPreference = s; }
    public String getSavedRoomPreference() { return savedRoomPreference; }
    public void setSavedRoomPreference(String s) { this.savedRoomPreference = s; }

    public static class Booking {
        private String type;
        private String bookingId;
        private String date;
        private int quantity;
        private double totalPrice;
        private List<String> selectedSeats = new ArrayList<>();
        private String selectedRoomType;
        private String bookingStatus = "CONFIRMED";
        private String cancellationReason;
        private String cancelledAt;
        private String departureTime;
        private double refundAmount;
        private String refundStatus;
        private String refundRequestedAt;
        private String refundCompletedAt;
        private String refundEta;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getBookingId() { return bookingId; }
        public void setBookingId(String bookingId) { this.bookingId = bookingId; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getTotalPrice() { return totalPrice; }
        public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
        public List<String> getSelectedSeats() { return selectedSeats; }
        public void setSelectedSeats(List<String> s) { this.selectedSeats = s; }
        public String getSelectedRoomType() { return selectedRoomType; }
        public void setSelectedRoomType(String s) { this.selectedRoomType = s; }
        public String getBookingStatus() { return bookingStatus; }
        public void setBookingStatus(String s) { this.bookingStatus = s; }
        public String getCancellationReason() { return cancellationReason; }
        public void setCancellationReason(String s) { this.cancellationReason = s; }
        public String getCancelledAt() { return cancelledAt; }
        public void setCancelledAt(String s) { this.cancelledAt = s; }
        public String getDepartureTime() { return departureTime; }
        public void setDepartureTime(String s) { this.departureTime = s; }
        public double getRefundAmount() { return refundAmount; }
        public void setRefundAmount(double r) { this.refundAmount = r; }
        public String getRefundStatus() { return refundStatus; }
        public void setRefundStatus(String s) { this.refundStatus = s; }
        public String getRefundRequestedAt() { return refundRequestedAt; }
        public void setRefundRequestedAt(String s) { this.refundRequestedAt = s; }
        public String getRefundCompletedAt() { return refundCompletedAt; }
        public void setRefundCompletedAt(String s) { this.refundCompletedAt = s; }
        public String getRefundEta() { return refundEta; }
        public void setRefundEta(String s) { this.refundEta = s; }
    }
}
