package com.makemytrip.makemytrip.models;

public enum CancellationReason {
    CHANGE_OF_PLANS("Change of plans"),
    FOUND_BETTER_PRICE("Found a better price elsewhere"),
    MEDICAL_EMERGENCY("Medical emergency"),
    FLIGHT_RESCHEDULED("Flight rescheduled / delayed by airline"),
    BOOKED_BY_MISTAKE("Booked by mistake"),
    PERSONAL_REASONS("Personal reasons"),
    OTHER("Other");

    private final String label;
    CancellationReason(String label) { this.label = label; }
    public String getLabel() { return label; }
}
