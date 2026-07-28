package model;

// A single parking slot on a floor, e.g. code "A-01", type CAR, status FREE.
public class Slot {
    private int id;
    private int floorId;
    private String code;
    private VehicleType vehicleType;
    private SlotStatus status;

    public Slot(int id, int floorId, String code, VehicleType vehicleType, SlotStatus status) {
        this.id = id;
        this.floorId = floorId;
        this.code = code;
        this.vehicleType = vehicleType;
        this.status = status;
    }

    public int getId() { return id; }
    public int getFloorId() { return floorId; }
    public String getCode() { return code; }
    public VehicleType getVehicleType() { return vehicleType; }
    public SlotStatus getStatus() { return status; }

    public void setStatus(SlotStatus status) { this.status = status; }

    public boolean isFree() { return status == SlotStatus.FREE; }
    public boolean isOccupied() { return status == SlotStatus.OCCUPIED; }
    public boolean isDisabled() { return status == SlotStatus.DISABLED; }
}
