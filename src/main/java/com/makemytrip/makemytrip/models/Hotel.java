package com.makemytrip.makemytrip.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "hotels")
public class Hotel {
    @Id
    private String _id;
    private String hotelName;
    private String location;
    private double pricePerNight;
    private int availableRooms;
    private String amenities;
    private List<String> imageUrls = new ArrayList<>();

    // ── NEW: Room types ───────────────────────────────────────
    // Auto-generated from availableRooms on first seat-map call
    // STANDARD (60%) | DELUXE (30%) | SUITE (10%)
    private List<RoomType> roomTypes = new ArrayList<>();

    public static class RoomType {
        private String type;         // "STANDARD" | "DELUXE" | "SUITE"
        private String colorHex;
        private double priceMultiplier; // 1.0 | 1.5 | 2.0
        private int totalRooms;
        private int availableRooms;
        private String description;
        // Simple floorplan SVG type identifier for frontend
        // "STANDARD" | "DELUXE" | "SUITE"
        private String floorplanType;

        public String getType() { return type; }
        public void setType(String t) { this.type = t; }
        public String getColorHex() { return colorHex; }
        public void setColorHex(String c) { this.colorHex = c; }
        public double getPriceMultiplier() { return priceMultiplier; }
        public void setPriceMultiplier(double p) { this.priceMultiplier = p; }
        public int getTotalRooms() { return totalRooms; }
        public void setTotalRooms(int t) { this.totalRooms = t; }
        public int getAvailableRooms() { return availableRooms; }
        public void setAvailableRooms(int a) { this.availableRooms = a; }
        public String getDescription() { return description; }
        public void setDescription(String d) { this.description = d; }
        public String getFloorplanType() { return floorplanType; }
        public void setFloorplanType(String f) { this.floorplanType = f; }
    }

    // existing getters/setters
    public String getId() { return _id; }
    public void setId(String id) { this._id = id; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public String getAmenities() { return amenities; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getAvailableRooms() { return availableRooms; }
    public void setAvailableRooms(int availableRooms) { this.availableRooms = availableRooms; }
    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    // new getters/setters
    public List<RoomType> getRoomTypes() { return roomTypes; }
    public void setRoomTypes(List<RoomType> r) { this.roomTypes = r; }
    public List<String> getImageUrls(){return imageUrls;}
    public void setImageUrls(List<String> i) {this.imageUrls = i;}
}
