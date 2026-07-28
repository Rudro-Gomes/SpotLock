package service;

import dao.RateDAO;
import model.VehicleType;

import java.time.Duration;
import java.time.LocalDateTime;

// Works out how much a booking costs: parked hours (rounded up, minimum one)
// multiplied by the hourly rate for the slot's vehicle type.
public class BillingService {

    private final RateDAO rateDAO;

    public BillingService(RateDAO rateDAO) {
        this.rateDAO = rateDAO;
    }

    // Number of chargeable hours between entry and exit. Any started hour counts
    // as a full hour, and every stay is billed at least one hour.
    public long computeChargeableHours(LocalDateTime entryTime, LocalDateTime exitTime) {
        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        if (minutes < 0) {
            minutes = 0;
        }
        long hours = (minutes + 59) / 60;   // ceiling division
        return Math.max(1, hours);
    }

    // Total fee = chargeable hours x the hourly rate for this vehicle type.
    public double computeFee(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime) {
        long hours = computeChargeableHours(entryTime, exitTime);
        double hourlyRate = rateDAO.getHourlyRate(vehicleType);
        return hours * hourlyRate;
    }
}
