package model;

import java.time.LocalDateTime;

// One parking booking: which vehicle took which slot, when it entered, when it
// is expected to leave, when it actually left, and how much it paid.
public class Booking {

    // Booking lifecycle status values (stored in the booking.status column).
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private int id;
    private int slotId;
    private String vehicleNo;
    private LocalDateTime entryTime;
    private LocalDateTime expectedExit;
    private LocalDateTime actualExit;   // null while still parked
    private double amount;              // 0 until checkout
    private String status;

    // Constructor used when starting a brand-new active booking (no id yet).
    public Booking(int slotId, String vehicleNo, LocalDateTime entryTime,
                   LocalDateTime expectedExit) {
        this.slotId = slotId;
        this.vehicleNo = vehicleNo;
        this.entryTime = entryTime;
        this.expectedExit = expectedExit;
        this.status = STATUS_ACTIVE;
    }

    // Constructor used when reading a full row back from the database.
    public Booking(int id, int slotId, String vehicleNo, LocalDateTime entryTime,
                   LocalDateTime expectedExit, LocalDateTime actualExit,
                   double amount, String status) {
        this.id = id;
        this.slotId = slotId;
        this.vehicleNo = vehicleNo;
        this.entryTime = entryTime;
        this.expectedExit = expectedExit;
        this.actualExit = actualExit;
        this.amount = amount;
        this.status = status;
    }

    public int getId() { return id; }
    public int getSlotId() { return slotId; }
    public String getVehicleNo() { return vehicleNo; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExpectedExit() { return expectedExit; }
    public LocalDateTime getActualExit() { return actualExit; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }

    public void setId(int id) { this.id = id; }
    public void setActualExit(LocalDateTime actualExit) { this.actualExit = actualExit; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return STATUS_ACTIVE.equals(status); }
}
