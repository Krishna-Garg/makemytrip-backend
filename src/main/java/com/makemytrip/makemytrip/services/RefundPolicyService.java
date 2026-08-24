package com.makemytrip.makemytrip.services;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class RefundPolicyService {
    public static class RefundResult{
        public double refundAmount;
        public double refundPercentage;
        public String tierDescription;
        public String estimatedEta;
    }
/**
 * departureTime and now are both LocalDateTime.
 * Tiers:
 *  > 7 days before departure   -> 100%
 *  1-7 days before departure   -> 75%
 *  < 24 hours before departure -> 50%
 *  at/after departure          -> 0%
 */
    public RefundResult calculateRefund(double totalPrice, LocalDateTime departureTime, LocalDateTime now){
        RefundResult result = new RefundResult();

        long hoursUntilDeparture = ChronoUnit.HOURS.between(now, departureTime);

        if (hoursUntilDeparture <= 0){
            result.refundPercentage = 0;
            result.tierDescription = "Cancelled at or after departure - no refund";
            result.estimatedEta = "N/A";
        } else if (hoursUntilDeparture < 24) {
            result.refundPercentage = 50;
            result.tierDescription = "Cancelled within 24 hours of departure - 50% refund";
            result.estimatedEta = "7-10 business days";
        } else if (hoursUntilDeparture <= 24*7) {
            result.refundPercentage = 75;
            result.tierDescription = "Cancelled in 1-7 days before departure - 75% refund";
            result.estimatedEta = "5-7 business days";
        } else {
            result.refundPercentage = 90;
            result.tierDescription = "Cancelled more than 7 days before the departure - 90% refund";
            result.estimatedEta = "3-5 business days";
        }
        result.refundAmount = totalPrice * (result.refundPercentage / 100.0);
        return result;
    }




}
