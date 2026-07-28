package service;

import concurrent.SlotLockManager;
import dao.BookingDAO;
import dao.FloorDAO;
import dao.SlotDAO;
import exception.BookingNotFoundException;
import exception.DataAccessException;
import exception.InvalidVehicleException;
import exception.SlotOccupiedException;
import model.Booking;
import model.Floor;
import model.Slot;
import util.AppLogger;
import util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

// The heart of ParkVault. Coordinates the DAOs, the per-slot locks, and the
// billing service to run the booking, checkout, search and auto-release flows.
// Every write flow is wrapped in a database transaction and guarded by the
// atomic slot update, so bookings stay correct under concurrency.
public class ParkingService {

    // A plate is 3-20 characters of letters, digits, spaces or hyphens, and
    // must start with a letter or digit.
    private static final Pattern VEHICLE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9 -]{2,19}$");

    private final FloorDAO floorDAO;
    private final SlotDAO slotDAO;
    private final BookingDAO bookingDAO;
    private final BillingService billingService;
    private final SlotLockManager slotLockManager;

    public ParkingService(FloorDAO floorDAO, SlotDAO slotDAO, BookingDAO bookingDAO,
                          BillingService billingService, SlotLockManager slotLockManager) {
        this.floorDAO = floorDAO;
        this.slotDAO = slotDAO;
        this.bookingDAO = bookingDAO;
        this.billingService = billingService;
        this.slotLockManager = slotLockManager;
    }

    // ---------- Simple reads ----------

    public List<Floor> getAllFloors() {
        return floorDAO.getAllFloors();
    }

    public List<Slot> getSlotsForFloor(int floorId) {
        return slotDAO.getSlotsForFloor(floorId);
    }

    public List<String[]> getHistoryRows() {
        return bookingDAO.getHistoryRows();
    }

    // ---------- Booking ----------

    // Books one slot for a vehicle. The per-slot lock serialises threads inside
    // this JVM; the atomic UPDATE inside a transaction is the real arbiter, so
    // if two threads still race, exactly one wins and the other gets a
    // SlotOccupiedException.
    public Booking bookSlot(int slotId, String vehicleNo, int expectedHours)
            throws SlotOccupiedException, InvalidVehicleException {

        String plate = validateVehicleNo(vehicleNo);
        if (expectedHours <= 0) {
            throw new IllegalArgumentException("Expected hours must be at least 1.");
        }

        boolean locked = false;
        Connection conn = null;
        try {
            locked = slotLockManager.tryLock(slotId);
            if (!locked) {
                throw new SlotOccupiedException("Slot is busy right now, please try again.");
            }

            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Authoritative guard: only succeeds if the slot was still FREE.
            if (!slotDAO.tryOccupySlot(conn, slotId)) {
                conn.rollback();
                throw new SlotOccupiedException("Slot is already occupied by another vehicle.");
            }

            LocalDateTime entryTime = LocalDateTime.now();
            LocalDateTime expectedExit = entryTime.plusHours(expectedHours);
            Booking booking = new Booking(slotId, plate, entryTime, expectedExit);
            int bookingId = bookingDAO.insertActiveBooking(conn, booking);
            booking.setId(bookingId);

            conn.commit();
            return booking;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DataAccessException("Booking failed for slot " + slotId, e);
        } finally {
            closeQuietly(conn);
            if (locked) {
                slotLockManager.unlock(slotId);
            }
        }
    }

    // ---------- Checkout ----------

    // Checks a vehicle out of a slot: finds its active booking, computes the fee,
    // marks the booking COMPLETED and frees the slot - all in one transaction.
    public Receipt checkoutSlot(int slotId) throws BookingNotFoundException {

        boolean locked = false;
        Connection conn = null;
        try {
            locked = slotLockManager.tryLock(slotId);
            if (!locked) {
                throw new BookingNotFoundException("Slot is busy right now, please try again.");
            }

            Booking booking = bookingDAO.findActiveBySlot(slotId);
            if (booking == null) {
                throw new BookingNotFoundException("No active booking found for this slot.");
            }

            Slot slot = slotDAO.getSlotById(slotId);
            LocalDateTime exitTime = LocalDateTime.now();
            long hours = billingService.computeChargeableHours(booking.getEntryTime(), exitTime);
            double amount = billingService.computeFee(slot.getVehicleType(),
                    booking.getEntryTime(), exitTime);

            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            bookingDAO.completeBooking(conn, booking.getId(), exitTime, amount);
            slotDAO.freeSlot(conn, slotId);
            conn.commit();

            return new Receipt(slot.getCode(), booking.getVehicleNo(),
                    booking.getEntryTime(), exitTime, hours, amount);

        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DataAccessException("Checkout failed for slot " + slotId, e);
        } finally {
            closeQuietly(conn);
            if (locked) {
                slotLockManager.unlock(slotId);
            }
        }
    }

    // ---------- Vehicle search ----------

    // "Where is my vehicle?" Finds the active booking for a plate and reports
    // the floor and slot it is parked in.
    public VehicleLocation locateVehicle(String vehicleNo) throws BookingNotFoundException {
        String plate = vehicleNo == null ? "" : vehicleNo.trim().toUpperCase();
        Booking booking = bookingDAO.findActiveByVehicleNo(plate);
        if (booking == null) {
            throw new BookingNotFoundException(
                    "No parked vehicle found with number \"" + plate + "\".");
        }

        Slot slot = slotDAO.getSlotById(booking.getSlotId());
        String floorName = findFloorName(slot.getFloorId());
        return new VehicleLocation(floorName, slot.getCode(), plate,
                booking.getEntryTime(), booking.getExpectedExit());
    }

    // ---------- Auto-release (called by the background scheduler) ----------

    // Frees every slot whose booking has passed its expected exit time. Returns
    // how many slots were released. Each slot is handled in its own transaction
    // so one failure does not block the others.
    public int releaseExpiredBookings() {
        List<Booking> expired = bookingDAO.findExpiredActiveBookings(LocalDateTime.now());
        int released = 0;

        for (Booking booking : expired) {
            int slotId = booking.getSlotId();
            boolean locked = false;
            Connection conn = null;
            try {
                locked = slotLockManager.tryLock(slotId);
                if (!locked) {
                    continue;   // busy now, will be retried on the next tick
                }

                Slot slot = slotDAO.getSlotById(slotId);
                double amount = billingService.computeFee(slot.getVehicleType(),
                        booking.getEntryTime(), booking.getExpectedExit());

                conn = DBConnection.getConnection();
                conn.setAutoCommit(false);
                bookingDAO.completeBooking(conn, booking.getId(),
                        booking.getExpectedExit(), amount);
                slotDAO.freeSlot(conn, slotId);
                conn.commit();
                released++;

            } catch (SQLException e) {
                rollbackQuietly(conn);
                AppLogger.error("Auto-release failed for slot " + slotId, e);
            } finally {
                closeQuietly(conn);
                if (locked) {
                    slotLockManager.unlock(slotId);
                }
            }
        }

        return released;
    }

    // ---------- Concurrency demo ----------

    // Fires many threads at ONE slot at the same moment to prove that only a
    // single booking can win. Resets the slot to FREE first so it is repeatable.
    public SimulationResult simulateConcurrentBookings(int slotId, int attempts) {
        resetSlotForSimulation(slotId);

        List<String> log = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startGate = new CountDownLatch(1);   // fires all threads together
        CountDownLatch doneGate = new CountDownLatch(attempts);

        for (int i = 1; i <= attempts; i++) {
            final int threadNo = i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    bookSlot(slotId, "SIM-" + threadNo, 1);
                    successCount.incrementAndGet();
                    log.add("Thread " + threadNo + ": SUCCESS - won the slot");
                } catch (SlotOccupiedException e) {
                    rejectedCount.incrementAndGet();
                    log.add("Thread " + threadNo + ": REJECTED - " + e.getMessage());
                } catch (Exception e) {
                    log.add("Thread " + threadNo + ": ERROR - " + e.getMessage());
                    AppLogger.error("Simulation thread " + threadNo + " failed", e);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();   // release all threads at once
        try {
            doneGate.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdownNow();

        return new SimulationResult(successCount.get(), rejectedCount.get(), log);
    }

    // Marks any active booking on the slot COMPLETED and sets it back to FREE,
    // so the concurrency demo can be run again and again.
    private void resetSlotForSimulation(int slotId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            Booking active = bookingDAO.findActiveBySlot(slotId);
            if (active != null) {
                bookingDAO.completeBooking(conn, active.getId(), LocalDateTime.now(), 0.0);
            }
            slotDAO.freeSlot(conn, slotId);
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            throw new DataAccessException("Could not reset slot " + slotId + " for the demo", e);
        } finally {
            closeQuietly(conn);
        }
    }

    // ---------- Helpers ----------

    // Validates and normalises a plate to uppercase, or throws.
    private String validateVehicleNo(String vehicleNo) throws InvalidVehicleException {
        if (vehicleNo == null || vehicleNo.trim().isEmpty()) {
            throw new InvalidVehicleException("Vehicle number cannot be empty.");
        }
        String plate = vehicleNo.trim().toUpperCase();
        if (!VEHICLE_PATTERN.matcher(plate).matches()) {
            throw new InvalidVehicleException(
                    "\"" + plate + "\" is not a valid vehicle number.");
        }
        return plate;
    }

    private String findFloorName(int floorId) {
        for (Floor floor : floorDAO.getAllFloors()) {
            if (floor.getId() == floorId) {
                return floor.getName();
            }
        }
        return "Floor " + floorId;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // nothing more we can do here
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
                // nothing more we can do here
            }
        }
    }

    // ---------- Small result holders returned to the UI ----------

    // The outcome of a checkout, ready to show as a receipt.
    public static class Receipt {
        private final String slotCode;
        private final String vehicleNo;
        private final LocalDateTime entryTime;
        private final LocalDateTime exitTime;
        private final long hours;
        private final double amount;

        public Receipt(String slotCode, String vehicleNo, LocalDateTime entryTime,
                       LocalDateTime exitTime, long hours, double amount) {
            this.slotCode = slotCode;
            this.vehicleNo = vehicleNo;
            this.entryTime = entryTime;
            this.exitTime = exitTime;
            this.hours = hours;
            this.amount = amount;
        }

        public String getSlotCode() { return slotCode; }
        public String getVehicleNo() { return vehicleNo; }
        public LocalDateTime getEntryTime() { return entryTime; }
        public LocalDateTime getExitTime() { return exitTime; }
        public long getHours() { return hours; }
        public double getAmount() { return amount; }
    }

    // Where a searched-for vehicle is parked.
    public static class VehicleLocation {
        private final String floorName;
        private final String slotCode;
        private final String vehicleNo;
        private final LocalDateTime entryTime;
        private final LocalDateTime expectedExit;

        public VehicleLocation(String floorName, String slotCode, String vehicleNo,
                               LocalDateTime entryTime, LocalDateTime expectedExit) {
            this.floorName = floorName;
            this.slotCode = slotCode;
            this.vehicleNo = vehicleNo;
            this.entryTime = entryTime;
            this.expectedExit = expectedExit;
        }

        public String getFloorName() { return floorName; }
        public String getSlotCode() { return slotCode; }
        public String getVehicleNo() { return vehicleNo; }
        public LocalDateTime getEntryTime() { return entryTime; }
        public LocalDateTime getExpectedExit() { return expectedExit; }
    }

    // Counts and per-thread log lines from the concurrency demo.
    public static class SimulationResult {
        private final int successCount;
        private final int rejectedCount;
        private final List<String> log;

        public SimulationResult(int successCount, int rejectedCount, List<String> log) {
            this.successCount = successCount;
            this.rejectedCount = rejectedCount;
            this.log = new ArrayList<>(log);
        }

        public int getSuccessCount() { return successCount; }
        public int getRejectedCount() { return rejectedCount; }
        public List<String> getLog() { return log; }
    }
}
