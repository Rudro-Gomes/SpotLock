package dao;

import exception.DataAccessException;
import model.Booking;
import util.DBConnection;
import util.TimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Reads and writes parking bookings. The insert and complete methods run on the
// caller's transaction connection so they commit together with the slot update.
public class BookingDAO {

    // Inserts a new ACTIVE booking and returns its generated id. Transactional.
    public int insertActiveBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "INSERT INTO booking "
                   + "(slot_id, vehicle_no, entry_time, expected_exit, status) "
                   + "VALUES (?, ?, ?, ?, 'ACTIVE')";

        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, booking.getSlotId());
            pstmt.setString(2, booking.getVehicleNo());
            pstmt.setString(3, TimeUtil.toDb(booking.getEntryTime()));
            pstmt.setString(4, TimeUtil.toDb(booking.getExpectedExit()));
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    // Marks a booking COMPLETED with its actual exit time and final amount.
    // Transactional.
    public void completeBooking(Connection conn, int bookingId,
                                LocalDateTime actualExit, double amount) throws SQLException {
        String sql = "UPDATE booking SET actual_exit = ?, amount = ?, "
                   + "status = 'COMPLETED' WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, TimeUtil.toDb(actualExit));
            pstmt.setDouble(2, amount);
            pstmt.setInt(3, bookingId);
            pstmt.executeUpdate();
        }
    }

    public Booking findActiveBySlot(int slotId) {
        String sql = baseSelect() + " WHERE slot_id = ? AND status = 'ACTIVE' "
                   + "ORDER BY id DESC LIMIT 1";
        return findOne(sql, slotId);
    }

    public Booking findActiveByVehicleNo(String vehicleNo) {
        String sql = baseSelect() + " WHERE vehicle_no = ? AND status = 'ACTIVE' "
                   + "ORDER BY id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicleNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapBooking(rs) : null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Could not search bookings for " + vehicleNo, e);
        }
    }

    // All active bookings whose expected exit time has already passed. Used by
    // the background ExpiryScheduler to auto-release overstayed slots.
    public List<Booking> findExpiredActiveBookings(LocalDateTime now) {
        List<Booking> expired = new ArrayList<>();
        String sql = baseSelect() + " WHERE status = 'ACTIVE' AND expected_exit <= ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, TimeUtil.toDb(now));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    expired.add(mapBooking(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Could not load expired bookings", e);
        }

        return expired;
    }

    // Display-ready rows for the History table, joined with slot and floor so
    // the user sees the slot code and floor name instead of raw ids.
    // Columns: ID, Vehicle, Floor, Slot, Entry, Expected Exit, Actual Exit,
    //          Amount, Status.
    public List<String[]> getHistoryRows() {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT b.id, b.vehicle_no, f.name AS floor_name, s.code AS slot_code, "
                   + "b.entry_time, b.expected_exit, b.actual_exit, b.amount, b.status "
                   + "FROM booking b "
                   + "JOIN slot s ON b.slot_id = s.id "
                   + "JOIN floor f ON s.floor_id = f.id "
                   + "ORDER BY b.id DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String amount = rs.getObject("amount") == null
                        ? "-"
                        : "Tk." + String.format("%.2f", rs.getDouble("amount"));
                rows.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("vehicle_no"),
                        rs.getString("floor_name"),
                        rs.getString("slot_code"),
                        TimeUtil.display(TimeUtil.fromDb(rs.getString("entry_time"))),
                        TimeUtil.display(TimeUtil.fromDb(rs.getString("expected_exit"))),
                        TimeUtil.display(TimeUtil.fromDb(rs.getString("actual_exit"))),
                        amount,
                        rs.getString("status")
                });
            }

        } catch (SQLException e) {
            throw new DataAccessException("Could not load booking history", e);
        }

        return rows;
    }

    private Booking findOne(String sql, int slotId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, slotId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapBooking(rs) : null;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Could not load booking for slot " + slotId, e);
        }
    }

    private String baseSelect() {
        return "SELECT id, slot_id, vehicle_no, entry_time, expected_exit, "
             + "actual_exit, amount, status FROM booking";
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        return new Booking(
                rs.getInt("id"),
                rs.getInt("slot_id"),
                rs.getString("vehicle_no"),
                TimeUtil.fromDb(rs.getString("entry_time")),
                TimeUtil.fromDb(rs.getString("expected_exit")),
                TimeUtil.fromDb(rs.getString("actual_exit")),
                rs.getDouble("amount"),
                rs.getString("status"));
    }
}
